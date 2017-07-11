package controllers

import java.net.InetAddress
import java.util.concurrent.TimeUnit

import model.{AddressCache, AddressCacheExpiring}
import org.scalatest.{BeforeAndAfter, WordSpec}
import org.scalatestplus.play.PlaySpec
import play.api.mvc._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.HeaderNames
import play.api.libs.json._

import scala.concurrent.Future

class ApiControllerTest extends PlaySpec with GuiceOneAppPerSuite with BeforeAndAfter {

  private var cachePerTest: AddressCache = _
  private var controllerPerTest: ApiController = _
  protected def cache: AddressCache = synchronized { cachePerTest }
  protected def controller: ApiController = synchronized { controllerPerTest }

  before {
    synchronized {
      cachePerTest = new AddressCacheExpiring(2, TimeUnit.SECONDS)
      val components = app.injector.instanceOf(classOf[ControllerComponents])
      controllerPerTest = new ApiController(components, cachePerTest)
    }
  }

  private def localhostV4 = {
    InetAddress.getByAddress("localhost", Array[Byte](127, 0, 0, 1))
  }
  private def localhostV6 = {
    InetAddress.getByAddress("localhost", Array[Byte](0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,1))
  }

  "Api#peek" should {
    "returns 204 on empty cache" in {
      val result: Future[Result] = controller.peek.apply(FakeRequest(GET, "/v1/addresses"))
      assert(status(result) === 204)
      assert(contentAsString(result) === "")
    }

    "returns InetAddress converted to json on nonEmpty cache" in {
      cache.add(localhostV4)

      val result: Future[Result] = controller.peek.apply(FakeRequest(GET, "/v1/addresses"))
      assert(status(result) === 200)
      assert(contentAsJson(result) === JsObject(Map(
      "version" -> JsNumber(4),
      "host" -> JsString("localhost"),
      "address" -> JsArray(Seq(JsNumber(127), JsNumber(0), JsNumber(0), JsNumber(1)))
      )))
    }
  }


  "Api#add" should {
    "returns 400 on incorrect IP version" in {
      val result: Future[Result] = controller.add.apply(postRequest(version = 5))
      assert(status(result) === 400)
      assert(contentAsJson(result) === JsObject(Map(
        "status" -> JsString("badrequest")
      )))
    }

    "returns 400 on incorrect IP address" in {
      val result: Future[Result] = controller.add.apply(postRequest(address = Seq(127, 0, 0, 1, 1)))
      assert(status(result) === 400)
      assert(contentAsJson(result) === JsObject(Map(
        "status" -> JsString("badrequest")
      )))


    }

    "returns 201 on successful adding IPv4 address" in {
      val result: Future[Result] = controller.add.apply(postRequest())
      assert(status(result) === 201)
      assert(contentAsJson(result) === JsObject(Map(
        "status" -> JsString("added")
      )))
      assert(cache.peek().isDefined && cache.peek().get == localhostV4)
    }

    "returns 200 on adding duplicate IPv4 address" in {
      cache.add(localhostV4)

      val result: Future[Result] = controller.add.apply(postRequest())
      assert(status(result) === 200)
      assert(contentAsJson(result) === JsObject(Map(
        "status" -> JsString("exists")
      )))
    }

    "returns 201 on successful adding IPv6 address" in {
      val result: Future[Result] = controller.add.apply(postRequest(6, Seq(0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,1)))
      assert(status(result) === 201)
      assert(contentAsJson(result) === JsObject(Map(
        "status" -> JsString("added")
      )))
      assert(cache.peek().isDefined && cache.peek().get === localhostV6)
    }

    "returns 200 on adding duplicate IPv6 address" in {
      cache.add(localhostV6)

      val result: Future[Result] = controller.add.apply(postRequest(6, Seq(0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,1)))
      assert(status(result) === 200)
      assert(contentAsJson(result) === JsObject(Map(
        "status" -> JsString("exists")
      )))
    }
  }

  private def postRequest(version: Int = 4, address: Seq[Int] = Seq[Int](127, 0, 0, 1)) = {
    FakeRequest(POST, "/v1/addresses", FakeHeaders(Seq(HeaderNames.HOST -> "localhost")),
      JsObject(Map(
        "version" -> JsNumber(version), //Incorrect version
        "host" -> JsString("localhost"),
        "address" -> JsArray(address.map(JsNumber(_)))
      ))
    )
  }
}
