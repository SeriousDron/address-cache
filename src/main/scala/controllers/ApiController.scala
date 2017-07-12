package controllers

import java.net.InetAddress
import javax.inject._

import model._
import play.api.i18n.I18nSupport
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._

import scala.concurrent.Future

/** Address cache API controller. Provides add, remove, peek and take operations on cache
 *
 * @author Andrey Petrenko
 * @see [[http://docs.addresscacheapi.apiary.io/]]
 * @param components Controller helper components
 * @param cache      Address cache instance to work with
 */
class ApiController @Inject()(
  contexts: Contexts,
  components: ControllerComponents,
  cache: AddressCache) extends AbstractController(components) with I18nSupport {

  implicit val addressCacheContext = contexts.addressCacheContext

  /** Returns last added address or 204
   *
   * @return
   */
  def peek = Action.async {
    Future {
      cache.peek() match {
        case Some(address) => Ok(Json.toJson(address))
        case _ => NoContent
      }
    }(addressCacheContext)
  }

  /** Adds an address to the cache and returns result status
   *
   * In case of invalid JSON or address returns 401 with {"status":"badrequest"}
   *
   */
  def add = Action.async(parse.tolerantJson) { request: Request[JsValue] =>
    val jsAddress = request.body.validate[InetAddress]
    jsAddress.fold(
      errors => {
        Future.successful(BadRequest(Json.obj("status" -> "badrequest")))
      },
      address => {
        Future {
          if (cache.add(address)) {
            Created(Json.obj("status" -> "added"))
          } else {
            Ok(Json.obj("status" -> "exists"))
          }
        }(addressCacheContext)
      }
    )
  }

  /** Removes an address from the cache. For valid address always returns 204
   *
   * In case of invalid JSON or address returns 401 with {"status":"badrequest"}
   *
   */
  def remove = Action.async(parse.tolerantJson) { request: Request[JsValue] =>
    val jsAddress = request.body.validate[InetAddress]
    jsAddress.fold(
      errors => {
        Future.successful(BadRequest(Json.obj("status" -> "badrequest")))
      },
      address => {
        Future {
          cache.remove(address)
          NoContent
        }(addressCacheContext)
      }
    )
  }

  /** Returns and removes last address. Blocks and waits if the cache is empty
   *
   */
  def take = Action.async {
    Future {
      Ok(Json.toJson(cache.take()))
    }(addressCacheContext)
  }
}
