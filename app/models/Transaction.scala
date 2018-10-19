package models

/**
  * Created by bezalel on 10/09/2018.
  */
case class Transaction(id: String,
                       blockNum: Long,
                       blockTime: String,
                       expiration: Long,
                       pending: Boolean,
                       numActions: Int,
                       trxVote: Long,
                       irreversible: Boolean)

object TransactionJsonFormats{
  import play.api.libs.json._

  implicit val transactionFormat: OFormat[Transaction] = Json.format[Transaction]
}
