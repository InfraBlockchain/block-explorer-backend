package controllers

import javax.inject.Inject
import io.swagger.annotations._
import models.{Account, AccountPermission}
import models.AccountJsonFormats._
import models.AccountPermissionJsonFormats._
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}
import repositories.AccountRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by bezalel on 14/09/2018.
  */
@Api(value = "/accounts")
class AccountsController @Inject()(cc: ControllerComponents,
                                   accountRepo: AccountRepository) extends AbstractController(cc) {

  @ApiOperation(
    value = "Get an account info by account-name",
    response = classOf[Account]
  )
  @ApiResponses(Array(
    new ApiResponse(code = 404, message = "Account not found")
  )
  )
  def getAccountByName(@ApiParam(value = "The account name to fetch account info") accountName: String) = Action.async {
    accountRepo.getAccountByName(accountName).map { maybeAccount =>
      maybeAccount.map { account =>
        Ok(Json.toJson(account))
      }.getOrElse(NotFound)
    }
  }

  @ApiOperation(
    value = "Search all account permissions for which the specific public key (search parameter) is set as an authority",
    response = classOf[AccountPermission],
    responseContainer = "List"
  )
  def getAccountPermissionsByPubKey(@ApiParam(value = "The base58-encoded public key string") publicKey: String) = Action.async {
    accountRepo.getAccountPermissionsByPubKey(publicKey).map { permissions =>
      Ok(Json.toJson(permissions))
    }
  }

}
