package controllers

import java.net.InetAddress
import javax.inject._

import model._
import play.api.i18n.I18nSupport
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._

/** Address cache API controller. Provides add, remove, peek and take operations on cache
 *
 * @author Andrey Petrenko
 * @see [[http://docs.addresscacheapi.apiary.io/]]
 * @param components Controller helper components
 * @param cache      Address cache instance to work with
 */
class ApiController @Inject()(components: ControllerComponents, cache: AddressCache)
  extends AbstractController(components) with I18nSupport {

  /** Returns last added address or 204
   *
   * @return
   */
  def peek = Action {
    cache.peek() match {
      case Some(address) => Ok(Json.toJson(address))
      case _ => NoContent
    }
  }

  /** Adds an address to the cache and returns result status
   *
   * In case of invalid JSON or address returns 401 with {"status":"badrequest"}
   *
   */
  def add = Action(parse.tolerantJson) { request: Request[JsValue] =>
    val jsAddress = request.body.validate[InetAddress]
    jsAddress.fold(
      errors => {
        BadRequest(Json.obj("status" -> "badrequest"))
      },
      address => {
        if (cache.add(address)) {
          Created(Json.obj("status" -> "added"))
        } else {
          Ok(Json.obj("status" -> "exists"))
        }
      }
    )
  }

  /** Removes an address from the cache. For valid address always returns 204
   *
   * In case of invalid JSON or address returns 401 with {"status":"badrequest"}
   *
   */
  def remove = Action(parse.tolerantJson) { request: Request[JsValue] =>
    val jsAddress = request.body.validate[InetAddress]
    jsAddress.fold(
      errors => {
        BadRequest(Json.obj("status" -> "badrequest"))
      },
      address => {
        cache.remove(address)
        NoContent
      }
    )
  }

  /** Returns and removes last address. Blocks and waits if the cache is empty
   *
   */
  def take = Action {
    Ok(Json.toJson(cache.take()))
  }

}
