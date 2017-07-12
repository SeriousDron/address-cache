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

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

class ApiControllerTest extends PlaySpec with GuiceOneAppPerSuite with BeforeAndAfter {

  private var cachePerTest: AddressCache = _
  private var controllerPerTest: ApiController = _
  protected def cache: AddressCache = synchronized { cachePerTest }
  protected def controller: ApiController = synchronized { controllerPerTest }

  before {
    synchronized {
      cachePerTest = new AddressCacheExpiring(2, TimeUnit.SECONDS)
      val components = app.injector.instanceOf(classOf[ControllerComponents])
      val contexts = new Contexts {
        val addressCacheContext: ExecutionContext = ExecutionContext.Implicits.global
      }
      controllerPerTest = new ApiController(contexts, components, cachePerTest)
    }
  }

  "Api#peek" should {
    "return 204 on empty cache" in {
      val result: Future[Result] = controller.peek.apply(FakeRequest(routes.ApiController.peek()))
      assert(status(result) === 204)
      assert(contentAsString(result) === "")
    }

    "return InetAddress converted to json on nonEmpty cache" in {
      cache.add(localhostV4)

      val result: Future[Result] = controller.peek.apply(FakeRequest(routes.ApiController.peek()))
      assert(status(result) === 200)
      assert(contentAsJson(result) === localhostV4Json)
    }
  }

  "Api#add" should {
    "return 400 on incorrect IP version" in {
      val result: Future[Result] = controller.add.apply(requestWithBody(version = 5))
      assert(status(result) === 400)
      assert(contentAsJson(result) === JsObject(Map(
        "status" -> JsString("badrequest")
      )))
    }

    "return 400 on incorrect IP address" in {
      val result: Future[Result] = controller.add.apply(requestWithBody(address = Seq(127, 0, 0, 1, 1)))
      assert(status(result) === 400)
      assert(contentAsJson(result) === JsObject(Map(
        "status" -> JsString("badrequest")
      )))
    }

    "return 201 on successful adding IPv4 address" in {
      val result: Future[Result] = controller.add.apply(requestWithBody())
      assert(status(result) === 201)
      assert(contentAsJson(result) === JsObject(Map(
        "status" -> JsString("added")
      )))
      assert(cache.peek().isDefined && cache.peek().get == localhostV4)
    }

    "return 200 on adding duplicate IPv4 address" in {
      cache.add(localhostV4)

      val result: Future[Result] = controller.add.apply(requestWithBody())
      assert(status(result) === 200)
      assert(contentAsJson(result) === JsObject(Map(
        "status" -> JsString("exists")
      )))
    }

    "return 201 on successful adding IPv6 address" in {
      val result: Future[Result] = controller.add.apply(requestWithBody(
        version = 6,
        address =Seq(0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,1)
      ))
      assert(status(result) === 201)
      assert(contentAsJson(result) === JsObject(Map(
        "status" -> JsString("added")
      )))
      assert(cache.peek().isDefined && cache.peek().get === localhostV6)
    }

    "return 200 on adding duplicate IPv6 address" in {
      cache.add(localhostV6)

      val result: Future[Result] = controller.add.apply(requestWithBody(
        version = 6,
        address = Seq(0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,1)
      ))
      assert(status(result) === 200)
      assert(contentAsJson(result) === JsObject(Map(
        "status" -> JsString("exists")
      )))
    }
  }

  "Api#remove" should {
    "remove an address and returns 204" in {
      cache.add(localhostV4)
      val result = controller.remove.apply(requestWithBody(action = routes.ApiController.remove()))

      assert(status(result) === 204)
      assert(contentAsString(result) === "")
      assert(cache.peek().isEmpty)
    }

    "return 204 when address does not exist" in {
      assert(cache.peek().isEmpty)
      val result = controller.remove.apply(requestWithBody(action = routes.ApiController.remove()))
      assert(status(result) === 204)
      assert(contentAsString(result) === "")
      assert(cache.peek().isEmpty)
    }

    "400 on incorrect ip address" in {
      val result = controller.remove.apply(requestWithBody(action = routes.ApiController.remove(), version = 5))
      assert(status(result) === 400)
      assert(contentAsJson(result) === JsObject(Map(
        "status" -> JsString("badrequest")
      )))
    }
  }

  "Api#take" should {
    "return last added element as JSON" in {
      cache.add(localhostV4)
      val result = controller.take.apply(FakeRequest(routes.ApiController.take()))
      assert(status(result) === 200)
      assert(contentAsJson(result) === localhostV4Json)
    }

    "wait in case cache is empty" in {
      val future = controller.take.apply(FakeRequest(routes.ApiController.take()))
      Thread.sleep(50)
      assert(future.isCompleted === false)
      cache.add(localhostV4)
      Thread.sleep(50)
      assert(future.isCompleted)
      assert(status(future) === 200)
      assert(contentAsJson(future) === localhostV4Json)
    }
  }

  private def requestWithBody(action: Call = routes.ApiController.add(), version: Int = 4, address: Seq[Int] = Seq[Int](127, 0, 0, 1)) = {
    FakeRequest(action.method, action.url, FakeHeaders(Seq(HeaderNames.HOST -> "localhost")),
      JsObject(Map(
        "version" -> JsNumber(version), //Incorrect version
        "host" -> JsString("localhost"),
        "address" -> JsArray(address.map(JsNumber(_)))
      ))
    )
  }

  private def localhostV4Json = {
    JsObject(Map(
      "version" -> JsNumber(4),
      "host" -> JsString("localhost"),
      "address" -> JsArray(Seq(JsNumber(127), JsNumber(0), JsNumber(0), JsNumber(1)))
    ))
  }

  private def localhostV4 = {
    InetAddress.getByAddress("localhost", Array[Byte](127, 0, 0, 1))
  }
  private def localhostV6 = {
    InetAddress.getByAddress("localhost", Array[Byte](0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,1))
  }
}
