package controllers

import blockchain.YosemiteChainStatus
import javax.inject.Inject
import io.swagger.annotations._
import models.{Action, ActionRaw, ActionRawList}
import models.ActionJsonFormats._
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}
import repositories.ActionRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by bezalel on 29/08/2018.
  */
@Api(value = "/actions")
class ActionsController @Inject()(cc: ControllerComponents,
                                  actionRepo: ActionRepository,
                                  yosemiteChainStatus: YosemiteChainStatus) extends AbstractController(cc) {

  @ApiOperation(
    value = "Get a action data by action-id",
    response = classOf[ActionRaw]
  )
  @ApiResponses(Array(
    new ApiResponse(code = 404, message = "Action not found")
  )
  )
  def getActionById(@ApiParam(value = "The id (global_sequence) of the Action to fetch") actionId: Long) = Action.async {
    actionRepo.getActionById(actionId).map { maybeAction =>
      maybeAction.map { action =>
        Ok(Json.toJson(action))
      }.getOrElse(NotFound)
    }
  }


  @ApiOperation(
    value = "Get a action in a Transaction by transaction-id and action index",
    response = classOf[ActionRaw]
  )
  @ApiResponses(Array(
    new ApiResponse(code = 404, message = "Action not found")
  )
  )
  def getActionInTransaction(@ApiParam(value = "The transaction-id of the Action to fetch") transactionId: String,
                             @ApiParam(value = "The action index in transaction") idx: Int) = Action.async {
    actionRepo.getActionInTransaction(transactionId, idx).map { maybeAction =>
      maybeAction.map { actionRawJs =>
        Ok(actionRawJs)
      }.getOrElse(NotFound)
    }
  }


  @ApiOperation(
    value = "Search all Actions in a Transaction",
    response = classOf[ActionRaw],
    responseContainer = "List"
  )
  def getActionsInTransaction(@ApiParam(value = "The id of the Transaction to fetch actions") transactionId: String,
                              @ApiParam(value = "page number to fetch action list, first page = 1 (default : 1)") page: Int,
                              @ApiParam(value = "the number of actions in current page (default : 100)") size: Int) = Action.async {
    actionRepo.getActionsInTransaction(transactionId, page, size).map { actions =>
      Ok(Json.toJson(actions))
    }
  }


  @Deprecated
  @ApiOperation(
    value = "Search all Actions received by an account",
    response = classOf[ActionRawList]
  )
  def getActionsByAccount(@ApiParam(value = "The action receiver account name to fetch actions") account: String,
                          @ApiParam(value = "action receiver sequence number to start fetching actions (exclusive for reverse search, inclusive for forward search) (default : -1 : next sequence number of the last(recent) actions)") start: Long,
                          @ApiParam(value = "offset to the end sequence number to fetch actions (default : -50)") offset: Int) = Action.async {
    actionRepo.getReceivedActionsByAccount(account, start, offset).map { actions =>
      Ok(Json.obj("lastIrrBlkNum" -> yosemiteChainStatus.getLastIrreversibleBlockNum(),
        "actions" -> Json.toJson(actions)))
    }
  }


  @ApiOperation(
    value = "Search all Actions received by an account",
    response = classOf[ActionRawList]
  )
  def getReceivedActionsByAccount(@ApiParam(value = "The action receiver account name to fetch actions") account: String,
                                  @ApiParam(value = "action receiver sequence number to start fetching actions (exclusive for reverse search, inclusive for forward search) (default : -1 : next sequence number of the last(recent) actions)") start: Long,
                                  @ApiParam(value = "offset to the end sequence number to fetch actions (default : -50)") offset: Int) = Action.async {
    actionRepo.getReceivedActionsByAccount(account, start, offset).map { actions =>
      Ok(Json.obj("lastIrrBlkNum" -> yosemiteChainStatus.getLastIrreversibleBlockNum(),
        "actions" -> Json.toJson(actions)))
    }
  }


  @ApiOperation(
    value = "Search all Actions sent by an account",
    response = classOf[ActionRawList]
  )
  def getSentActionsByAccount(@ApiParam(value = "The action sender account name") account: String,
                              @ApiParam(value = "sent-action global sequence number to start fetching actions (exclusive for reverse search, inclusive for forward search) (default : -1 : next index number of the last(recent) actions)") start: Long,
                              @ApiParam(value = "offset item count to fetch actions (offset < 0 : reverse search, offset > 0 : forward search, default : -50)") offset: Int) = Action.async {
    actionRepo.getSentActionsByAccount(account, start, offset).map { actions =>
      Ok(Json.obj("lastIrrBlkNum" -> yosemiteChainStatus.getLastIrreversibleBlockNum(),
        "actions" -> Json.toJson(actions)))
    }
  }

}
