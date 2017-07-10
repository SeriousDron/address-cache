import java.lang.Thread.State
import java.net.InetAddress

import org.scalatest.{AsyncFunSuite, FunSuite}
import java.util.concurrent.{Callable, CountDownLatch, FutureTask, TimeUnit}

import scala.concurrent.{Future, Promise}

class AddressCacheConcurrencyTest extends AsyncFunSuite {

  test("`take` is locking when queue is empty and unlocking on `add`") {
    val addr = InetAddress.getLoopbackAddress
    val cache = new AddressCache(1, TimeUnit.SECONDS)

    val promise: Promise[InetAddress] = Promise()

    val blockingThread = new Thread(() => {
      promise.success(cache.take())
    })
    blockingThread.start()
    Thread.sleep(250)

    assert(blockingThread.getState === State.WAITING)
    cache.add(addr)

    promise.future map { result =>
      assert(result === addr)
    }
  }

  test("All locks successfully released") {
    val numThreads = 256
    val cache = new AddressCache(1, TimeUnit.SECONDS)

    val readersLatch = new CountDownLatch(numThreads)
    val writersLatch = new CountDownLatch(numThreads)

    for (i <- 0 until numThreads) {
      new Thread(() => {
        cache.take()
        readersLatch.countDown()
      }).start()
      new Thread(() => {
        cache.add(InetAddress.getByAddress(Array[Byte](127, 0, 0, i.toByte)))
        writersLatch.countDown()
      }).start()
    }

    Future {
      assert(writersLatch.await(2, TimeUnit.SECONDS))
      assert(readersLatch.await(2, TimeUnit.SECONDS))
    }
  }

  test("All thread blocked and then locks successfully released") {
    val numThreads = 256
    val cache = new AddressCache(1, TimeUnit.SECONDS)

    val readersLatch = new CountDownLatch(numThreads)
    val writersLatch = new CountDownLatch(numThreads)

    val threads = new Array[Thread](numThreads)
    for (i <- 0 until numThreads) {
      threads(i) = new Thread(() => {
        cache.take()
        readersLatch.countDown()
      })
      threads(i).start()
    }

    Thread.sleep(250)

    assert(threads.filterNot(_.getState == State.WAITING).length === 0)

    for (i <- 0 until numThreads) {
      new Thread(() => {
        cache.add(InetAddress.getByAddress(Array[Byte](127, 0, 0, i.toByte)))
        writersLatch.countDown()
      }).start()
    }

    Future {
      assert(writersLatch.await(2, TimeUnit.SECONDS))
      assert(readersLatch.await(2, TimeUnit.SECONDS))
    }
  }

  test("Concurrent `add` doesn't produce duplicates") {
    val numThreads = 256
    val cache = new AddressCache(1, TimeUnit.SECONDS)

    val writersLatch = new CountDownLatch(numThreads)
    for (i <- 0 until numThreads) {
      new Thread(() => {
        cache.add(InetAddress.getLoopbackAddress)
        writersLatch.countDown()
      }).start()
    }

    Future {
      assert(writersLatch.await(2, TimeUnit.SECONDS))
      var count = 0
      while(cache.peek().isDefined) {
        cache.take()
        count += 1
      }
      assert(count === 1)
    }
  }
}
