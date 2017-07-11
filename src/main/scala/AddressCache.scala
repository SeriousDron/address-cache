import java.net.InetAddress
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.{Condition, ReadWriteLock, ReentrantReadWriteLock}

import scala.collection.mutable

trait Cache[E] {

  /** Adds element in cache and returns if it was successfully added
   *
   * @param el Element to add
   */
  def add(el: E): Boolean

  /** Removes element from cache and returns if it was successfully removed
   *
   * @param el Element to remove
   */
  def remove(el: E): Boolean

  /** Returns last added element or None
   *
   * @return Last element added or None
   */
  def peek(): Option[E]

  /**
   * Returns last element added. Blocks and wait if there are no elements
   *
   */
  def take(): E
}

/** Cache for InetAddress objects with given expiration and stack-like access
 *
 * Expects to have much more reads than write so do cleanup on writes
 *
 * @author Andrey Petrenko
 */
class AddressCache(maxAge: Long, unit: TimeUnit) extends Cache[InetAddress] {

  if (maxAge <= 0) throw new IllegalArgumentException("Cache TTL should be positive number")

  private val expiresAfter: Long = unit.toNanos(maxAge)

  /** Our main lock too add synchronisation on top of LinkedHashSet
   *
   * Using ReadWriteLock as in real world cache is likely to have much more requests on read and we want read threads
   * do not block each other
   */
  private val rwLock: ReadWriteLock = new ReentrantReadWriteLock()
  private val notEmpty: Condition = rwLock.writeLock().newCondition()
  private val items: mutable.LinkedHashSet[Entry] = mutable.LinkedHashSet[Entry]()

  /** Entry of cache. Wraps an address with expiration time
   *
   * @param address   Address to store
   * @param expiresAt Expiration time in nanos
   */
  private case class Entry(address: InetAddress, expiresAt: Long = System.nanoTime() + expiresAfter) {
    /** Using address hashcode so same address have same hash and expiresAt doesn't matter */
    override def hashCode(): Int = address.hashCode()

    /** Comparin entries basing on address only as expiresAt doesn't matter */
    override def equals(obj: Any): Boolean = obj match {
      //We are not taking expiration time into account when comparing
      case Entry(addr, _) => address == addr
      case _ => false
    }

    def isExpired: Boolean = expiresAt < System.nanoTime()
  }

  /** Adds element to cache. Ignores existing elements
   *
   * Does cleanup before adding
   *
   * @param address Address to add. Ignores if address is already in cache
   * @return true if element was successfully added or false if already existed
   */
  override def add(address: InetAddress): Boolean = {
    val entry = Entry(address)
    rwLock.writeLock().lock()
    try {
      cleanup()
      val added: Boolean = items.add(entry) //Return false is element already exists
      if (added) {
        notEmpty.signal()
      }
      added
    } finally {
      rwLock.writeLock().unlock()
    }
  }

  /** Removes element from cache and returns true. In case element is not in cache ignores (as you reached your desired state)
   *
   * Does cleanup before removing
   *
   * @param address Address to remove from cache
   */
  override def remove(address: InetAddress): Boolean = {
    val entry = Entry(address)
    rwLock.writeLock().lock()
    try {
      cleanup()
      items.remove(entry)
      true
    } finally {
      rwLock.writeLock().unlock()
    }
  }

  /** Optionally returns last element added to cache */
  override def peek(): Option[InetAddress] = {
    rwLock.readLock().lock()
    try {
      val last = items.lastOption
      if (last.isEmpty || !last.get.isExpired) {
        //Otherwise all entries are expired and we need to cleanup
        return items.lastOption map (_.address)
      }
    } finally {
      rwLock.readLock().unlock()
    }
    //We need to release read lock before trying to acquire write. So cleanup is after finally
    cleanAll()
    None
  }

  /** Returns and removes last element added to cache. Blocks in case cache is empty until first element to come */
  override def take(): InetAddress = {
    def allExpiredWithClean: Boolean = if (items.last.isExpired) {
      cleanAll(); true
    } else false

    rwLock.writeLock().lock()
    try {
      while (items.isEmpty || allExpiredWithClean) {
        notEmpty.await()
      }
      val last = items.last
      items.remove(last)
      last.address
    } finally {
      rwLock.writeLock().unlock()
    }
  }

  /** Optimized method for cleaning whole cache
   *
   * Should be called in very rare case when on read we've found that all entries are expired
   */
  protected def cleanAll(): Unit = {
    rwLock.writeLock().lock()
    try {
      if (items.nonEmpty && items.last.isExpired) {
        items.clear()
      }
    } finally {
      rwLock.writeLock().unlock()
    }
  }

  /** Cleans up expired elements from the cache  */
  protected def cleanup(): Unit = {
    rwLock.writeLock().lock()
    try {
      while (items.nonEmpty && items.head.isExpired) {
        items.remove(items.head)
      }
    } finally {
      rwLock.writeLock().unlock()
    }
  }
}