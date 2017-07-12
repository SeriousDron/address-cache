package controllers

import java.net.InetAddress
import javax.inject._

import model._
import play.api.i18n.I18nSupport
import play.api.libs.json.{JsError, JsValue, Json}
import play.api.mvc._

/** Address cache API controller. Provides add, remove, peek and take operations on cache
 *
 * @see [[http://docs.addresscacheapi.apiary.io/]]
 *
 * @param components Controller helper components
 * @param cache Address cache instance to work with
 */
class ApiController @Inject()(components: ControllerComponents, cache: AddressCache)
  extends AbstractController(components) with I18nSupport {

  def peek = TODO

  def add = Action(parse.tolerantJson) { request: Request[JsValue] => //"TODO" doesn't work with requirement of Json
    NotImplemented
  }

  def remove = Action(parse.tolerantJson) { request: Request[JsValue] =>  //"TODO" doesn't work with requirement of Json
    NotImplemented
  }

  def take = TODO

}
