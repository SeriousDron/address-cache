import java.lang.Thread.State
import java.net.InetAddress
import java.util.concurrent.TimeUnit

import model.AddressCacheExpiring
import org.scalatest.FunSuite

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Promise}


class AddressCacheExpirationTest extends FunSuite {

  test("Expired address is not peekable") {
    val addr = InetAddress.getLoopbackAddress
    val cache = new AddressCacheExpiring(50, TimeUnit.MILLISECONDS)

    cache.add(addr)
    Thread.sleep(100)
    assert(cache.peek().isEmpty)
  }

  test("`take` doesn't return expired addresses") {
    val addr = InetAddress.getLoopbackAddress
    val addr2 = InetAddress.getByAddress(Array[Byte](127, 0, 0, 2))
    val cache = new AddressCacheExpiring(50, TimeUnit.MILLISECONDS)

    val promise = Promise[InetAddress]()
    cache.add(addr)
    Thread.sleep(55)

    val takeThread = new Thread(() => {promise.success(cache.take())})
    takeThread.start()

    Thread.sleep(55)
    assert(takeThread.getState == State.WAITING)
    cache.add(addr2)
    assert(Await.result(promise.future, Duration(50, TimeUnit.MILLISECONDS)) === addr2)
  }

  test("`add` will add element once again after expiration") {
    val addr = InetAddress.getLoopbackAddress
    val cache = new AddressCacheExpiring(50, TimeUnit.MILLISECONDS)

    cache.add(addr)
    assert(cache.add(addr) === false)
    Thread.sleep(55)
    assert(cache.add(addr) === true)
  }

  test("`remove` and add refreshes expiration time") {
    val addr = InetAddress.getLoopbackAddress
    val cache = new AddressCacheExpiring(50, TimeUnit.MILLISECONDS)

    cache.add(addr)
    Thread.sleep(25)
    assert(cache.remove(addr) === true)
    assert(cache.add(addr) === true)
    Thread.sleep(30)
    assert(cache.peek().isDefined)
    Thread.sleep(30)
    assert(cache.peek().isEmpty)
  }
}
