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
 * @author Andrey Petrenko
 */
class AddressCache(maxAge: Long, unit: TimeUnit) extends Cache[InetAddress] {

  /** Adds element to cache. Ignores existing elements
   *
   * @param address Address to add. Ignores if address is already in cache
   * @return true if element was successfully added or false if already existed
   */
  override def add(address: InetAddress): Boolean = ???

  /** Removes element from cache and returns true. In case element is not in cache ignores (as you reached your desired state)
   *
   * @param address Address to remove from cache
   */
  override def remove(address: InetAddress): Boolean = ???

  /** Optionally returns last element added to cache
   */
  override def peek(): Option[InetAddress] = ???

  /** Returns and removes last element added to cache. Blocks in case cache is empty until first element to come
   *
   * @return
   */
  override def take(): InetAddress = ???
}