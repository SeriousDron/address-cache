package model

import java.net.InetAddress

import org.scalatest.FunSuite
import play.api.libs.json._
import model._

class InetAddressJson extends FunSuite{

  test("ipv4 is correctly converted to JSON") {
    val address = InetAddress.getByAddress("localhost", Array[Byte](127, 0, 0, 1))
    assert(Json.toJson(address) === JsObject(Map(
      "version" -> JsNumber(4),
      "host" -> JsString("localhost"),
      "address" -> JsArray(Seq(JsNumber(127), JsNumber(0), JsNumber(0), JsNumber(1)))
    )))
  }

  test("ipv6 is correctly converted to JSON") {
    val address = InetAddress.getByAddress("localhost", Array[Byte](0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,1))
    assert(Json.toJson(address) === JsObject(Map(
      "version" -> JsNumber(6),
      "host" -> JsString("localhost"),
      "address" -> JsArray(Seq(
        JsNumber(0), JsNumber(0), JsNumber(0), JsNumber(0),
        JsNumber(0), JsNumber(0), JsNumber(0), JsNumber(0),
        JsNumber(0), JsNumber(0), JsNumber(0), JsNumber(0),
        JsNumber(0), JsNumber(0), JsNumber(0), JsNumber(1)
      ))
    )))
  }

  test("correctly build ipv4 address") {
    val address = InetAddress.getByAddress("localhost", Array[Byte](127, 0, 0, 1))
    assert(Json.fromJson[InetAddress](JsObject(Map(
      "version" -> JsNumber(4),
      "host" -> JsString("localhost"),
      "address" -> JsArray(Seq(JsNumber(127), JsNumber(0), JsNumber(0), JsNumber(1)))
    ))).get === address)
  }

  test("correctly build ipv4 address without host") {
    val address = InetAddress.getByAddress(Array[Byte](127, 0, 0, 1))
    assert(Json.fromJson[InetAddress](JsObject(Map(
      "version" -> JsNumber(4),
      "address" -> JsArray(Seq(JsNumber(127), JsNumber(0), JsNumber(0), JsNumber(1)))
    ))).get === address)
  }

  test("correctly builds ipv6 address") {
    val address = InetAddress.getByAddress("localhost", Array[Byte](0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,1))
    assert(Json.fromJson[InetAddress](JsObject(Map(
      "version" -> JsNumber(6),
      "host" -> JsString("localhost"),
      "address" -> JsArray(Seq(
        JsNumber(0), JsNumber(0), JsNumber(0), JsNumber(0),
        JsNumber(0), JsNumber(0), JsNumber(0), JsNumber(0),
        JsNumber(0), JsNumber(0), JsNumber(0), JsNumber(0),
        JsNumber(0), JsNumber(0), JsNumber(0), JsNumber(1)
      ))
    ))).get === address)
  }

  test("correctly builds ipv6 address without host") {
    val address = InetAddress.getByAddress(Array[Byte](0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,1))
    assert(Json.fromJson[InetAddress](JsObject(Map(
      "version" -> JsNumber(6),
      "address" -> JsArray(Seq(
        JsNumber(0), JsNumber(0), JsNumber(0), JsNumber(0),
        JsNumber(0), JsNumber(0), JsNumber(0), JsNumber(0),
        JsNumber(0), JsNumber(0), JsNumber(0), JsNumber(0),
        JsNumber(0), JsNumber(0), JsNumber(0), JsNumber(1)
      ))
    ))).get === address)
  }
}
