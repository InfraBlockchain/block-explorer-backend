package controllers

import javax.inject.Inject
import io.swagger.annotations._
import models.TransactionJsonFormats._
import models.{Transaction, TransactionRepository}
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by bezalel on 29/08/2018.
  */
@Api(value = "/transactions")
class TransactionsController @Inject()(cc: ControllerComponents, transactionRepo: TransactionRepository) extends AbstractController(cc) {

  @ApiOperation(
    value = "Search all Transactions",
    response = classOf[Transaction],
    responseContainer = "List"
  )
  def getTransactions(@ApiParam(value = "page number to fetch recent transaction list, first page = 1 (default : 1)") page: Int,
                      @ApiParam(value = "the number of transactions in current page (default : 30)") size: Int) = Action.async {
    transactionRepo.getTransactions(page, size).map { transactions =>
      Ok(Json.toJson(transactions))
    }
  }


  @ApiOperation(
    value = "Get a transaction data by transaction-id",
    response = classOf[Transaction]
  )
  @ApiResponses(Array(
    new ApiResponse(code = 404, message = "Transaction not found")
  )
  )
  def getTransactionById(@ApiParam(value = "The id of the Transaction to fetch") transactionId: String) = Action.async {
    transactionRepo.getTransactionById(transactionId).map { maybeTx =>
      maybeTx.map { tx =>
        Ok(Json.toJson(tx))
      }.getOrElse(NotFound)
    }
  }

  @ApiOperation(
    value = "Search all Transactions in a Block",
    response = classOf[Transaction],
    responseContainer = "List"
  )
  def getTransactionsForBlock(@ApiParam(value = "The block number of the Block to fetch") blockNum: Long,
                              @ApiParam(value = "page number to fetch transaction list, first page = 1 (default : 1)") page: Int,
                              @ApiParam(value = "the number of transactions in current page (default : 30)") size: Int) = Action.async {
    transactionRepo.getTransactionsForBlock(blockNum, page, size).map { transactions =>
      Ok(Json.toJson(transactions))
    }
  }

}
