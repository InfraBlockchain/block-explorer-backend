package controllers

import javax.inject.Inject
import io.swagger.annotations._
import models.ActionJsonFormats._
import models.{Action, ActionRepository}
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by bezalel on 29/08/2018.
  */
@Api(value = "/actions")
class ActionsController @Inject()(cc: ControllerComponents, actionRepo: ActionRepository) extends AbstractController(cc) {

//  @ApiOperation(
//    value = "Search all Transactions",
//    response = classOf[Transaction],
//    responseContainer = "List"
//  )
//  def getTransactions(@ApiParam(value = "page number to fetch recent transaction list, first page = 1 (default : 1)") page: Int,
//                      @ApiParam(value = "the number of transactions in current page (default : 30)") size: Int) = Action.async {
//    transactionRepo.getTransactions(page, size).map { transactions =>
//      Ok(Json.toJson(transactions))
//    }
//  }


  @ApiOperation(
    value = "Get a action data by action-id",
    response = classOf[Action]
  )
  @ApiResponses(Array(
    new ApiResponse(code = 404, message = "Action not found")
  )
  )
  def getActionById(@ApiParam(value = "The id of the Action to fetch") actionId: String) = Action.async {
    actionRepo.getActionById(actionId).map { maybeAction =>
      maybeAction.map { action =>
        Ok(Json.toJson(action))
      }.getOrElse(NotFound)
    }
  }


  @ApiOperation(
    value = "Get a action in a Transaction by transaction-id and action index",
    response = classOf[Action]
  )
  @ApiResponses(Array(
    new ApiResponse(code = 404, message = "Action not found")
  )
  )
  def getActionInTransaction(@ApiParam(value = "The transaction-id of the Action to fetch") transactionId: String,
                             @ApiParam(value = "The action index in transaction") idx: Int) = Action.async {
    actionRepo.getActionInTransaction(transactionId, idx).map { maybeAction =>
      maybeAction.map { action =>
        Ok(Json.toJson(action))
      }.getOrElse(NotFound)
    }
  }


  @ApiOperation(
    value = "Search all Transactions in a Block",
    response = classOf[Action],
    responseContainer = "List"
  )
  def getActionsInTransaction(@ApiParam(value = "The id of the Transaction to fetch actions") transactionId: String,
                              @ApiParam(value = "page number to fetch action list, first page = 1 (default : 1)") page: Int,
                              @ApiParam(value = "the number of actions in current page (default : 30)") size: Int) = Action.async {
    actionRepo.getActionsInTransaction(transactionId, page, size).map { transactions =>
      Ok(Json.toJson(transactions))
    }
  }


}