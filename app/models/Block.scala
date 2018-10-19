package models

import play.api.libs.json.JsValue

/**
  * Created by bezalel on 10/09/2018.
  */
case class Block(id: String,
                 blockNumber: Long,
                 blockTime: String,
                 producer: String,
                 confirmed: Long,
                 prevBlockId: String,
                 transactionMerkleRoot: String,
                 actionMerkleRoot: String,
                 version: Int,
                 newProducers: Option[JsValue],
                 numTransactions: Int,
                 trxVotes: Long,
                 irreversible: Boolean)

object BlockJsonFormats{
  import play.api.libs.json._

  implicit val blockFormat: OFormat[Block] = Json.format[Block]
}