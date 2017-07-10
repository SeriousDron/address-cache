import java.net.InetAddress
import org.scalatest.FunSuite
import java.util.concurrent.TimeUnit


class AddressCacheTest extends FunSuite {

  test("Address cache adds object successfully") {
    val addr = InetAddress.getLoopbackAddress
    val cache = new AddressCache(5, TimeUnit.SECONDS)

    assert(cache.add(addr) === true)
    assert(cache.peek().isDefined)
    assert(cache.take() === addr)
  }

  test("Address cache ignore same object added twice") {
    val addr = InetAddress.getLoopbackAddress
    val cache = new AddressCache(5, TimeUnit.SECONDS)

    cache.add(addr)
    cache.add(addr)
    assert(cache.take() === addr)
    assert(cache.peek().isEmpty)
  }

  test("Address cache removes object successfully") {
    val addr = InetAddress.getLoopbackAddress
    val cache = new AddressCache(5, TimeUnit.SECONDS)

    cache.add(addr)
    assert(cache.peek().isDefined)
    cache.remove(addr)
    assert(cache.peek().isEmpty)
  }

  test("peek returns None on empty cache") {
    val cache = new AddressCache(5, TimeUnit.SECONDS)
    assert(cache.peek().isEmpty)
  }

  test("peek returns last added element if any") {
    val addr = InetAddress.getLoopbackAddress
    val addr2 = InetAddress.getByAddress(Array[Byte](0x7f, 0x00, 0x00, 0x02))
    val cache = new AddressCache(5, TimeUnit.SECONDS)
    cache.add(addr)
    cache.add(addr2)
    assert(cache.peek().isDefined)
    assert(cache.peek().get === addr2)
  }

  test("take returns last added element") {
    val addr = InetAddress.getLoopbackAddress
    val addr2 = InetAddress.getByAddress(Array[Byte](0x7f, 0x00, 0x00, 0x02))
    val cache = new AddressCache(5, TimeUnit.SECONDS)
    cache.add(addr)
    cache.add(addr2)
    assert(cache.take() === addr2)
  }

  test("take removes last added element") {
    val addr = InetAddress.getLoopbackAddress
    val addr2 = InetAddress.getByAddress(Array[Byte](0x7f, 0x00, 0x00, 0x02))
    val cache = new AddressCache(5, TimeUnit.SECONDS)
    cache.add(addr)
    cache.add(addr2)
    cache.take()
    assert(cache.peek().get === addr)
  }
}
