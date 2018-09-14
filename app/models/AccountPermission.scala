package models

/**
  * Created by bezalel on 14/09/2018.
  */
case class AccountPermission(account: String,
                             permission: String,
                             publicKey: String,
                             createdAt: Long)

object AccountPermissionJsonFormats{
  import play.api.libs.json._

  implicit val accountPermissionFormat: OFormat[AccountPermission] = Json.format[AccountPermission]
}
