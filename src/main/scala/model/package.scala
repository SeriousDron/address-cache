import java.net.{Inet4Address, Inet6Address, InetAddress}

import play.api.libs.json._
import play.api.libs.json.Reads._

import scala.util.Try

package object model {

  implicit val inetAddressWrites: Writes[InetAddress] = {
    case a: Inet4Address => Json.obj(
      "version" -> 4,
      "host" -> a.getHostName,
      "address" -> a.getAddress
    )
    case a: Inet6Address => Json.obj(
      "version" -> 6,
      "host" -> a.getHostName,
      "address" -> a.getAddress
    )
  }

  implicit val inetAddressReads: Reads[InetAddress] = (json: JsValue) => Try {
    for {
      version <- (json \ "version").validate[Int]
      correctVersion <- if (version == 4 || version == 6) JsSuccess(true)
      else JsError(JsPath \ "version", "IP address version is either 4 or 6")
      hostname <- (json \ "host").validateOpt[String]
      address <- (json \ "address").validate[Array[Byte]]
      success <- if ((version == 4 && address.length == 4) || (version == 6 && address.length == 16)) JsSuccess(true)
      else JsError(JsPath \ "address", s"Incorrect address length for version $version")
    } yield {
      if (hostname.isEmpty) {
        InetAddress.getByAddress(address)
      } else InetAddress.getByAddress(hostname.get, address)
    }
  }.getOrElse(JsError("Incorrect address"))

}
