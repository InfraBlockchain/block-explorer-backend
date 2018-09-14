package models

import play.api.libs.json.JsValue

/**
  * Created by bezalel on 14/09/2018.
  */
case class Account(name: String,
                   createdAt: Long,
                   updatedAt: Option[Long],
                   abi: Option[JsValue])

object AccountJsonFormats{
  import play.api.libs.json._

  implicit val accountFormat: OFormat[Account] = Json.format[Account]
}
