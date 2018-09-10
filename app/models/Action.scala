package models

import play.api.libs.json.JsValue

/**
  * Created by bezalel on 10/09/2018.
  */
case class Action(id: String,
                  transaction: String,
                  account: String,
                  name: String,
                  data: JsValue,
                  authorization: JsValue,
                  seq: Long,
                  parent: Long)

object ActionJsonFormats{
  import play.api.libs.json._

  implicit val actionFormat: OFormat[Action] = Json.format[Action]
}
