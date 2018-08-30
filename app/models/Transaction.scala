package models

import javax.inject.Inject
import org.joda.time.DateTime
import play.api.Logger

import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json._
import reactivemongo.api.{Cursor, ReadPreference}
import reactivemongo.play.json._
import reactivemongo.play.json.collection.JSONCollection
import play.modules.reactivemongo.ReactiveMongoApi

/**
  * Created by bezalel on 29/08/2018.
  */

case class Transaction(id: String,
                       expiration: Long,
                       refBlockPrefix: Long,
                       numActions: Int,
                       pending: Boolean,
                       createdAt: Long,
                       blockId: Long,
                       updatedAt: Long)

object TransactionJsonFormats{
  import play.api.libs.json._

  implicit val transactionFormat: OFormat[Transaction] = Json.format[Transaction]
}

class TransactionRepository @Inject()(implicit ec: ExecutionContext, reactiveMongoApi: ReactiveMongoApi){

  import TransactionJsonFormats._

//  def transactionTracesCollection: Future[JSONCollection] = reactiveMongoApi.database.map(_.collection("transaction_traces"))
  def transactionsCollection: Future[JSONCollection] = reactiveMongoApi.database.map(_.collection("transactions"))

  def _toTransaction(trxIdOpt: Option[String], doc: JsObject): Transaction = {
//    Logger.warn(doc.toString())

    val jsDoc = Json.toJson(doc)

    val trxId: String = trxIdOpt match {
      case Some(tid) => tid
      case _ => (jsDoc \ "trx_id").as[String]
    }

    val expirationStr = (jsDoc \ "expiration").as[String]
    val expiration : Long = DateTime.parse(expirationStr).toInstant.getMillis
    val refBlockPrefix = (jsDoc \ "ref_block_prefix").as[Long]
    val numActions = (jsDoc \ "actions").asOpt[JsArray].getOrElse(JsArray()).value.size
    val pending = (jsDoc \ "scheduled").as[Boolean]
    val createdAt = (jsDoc \ "createdAt" \ "$date").as[Long] / 1000
//    val blockId = (jsDoc \ "block_id").asOpt[String].getOrElse("")
    val blockId = (jsDoc \ "block_num").as[Long]
    val updatedAt = (jsDoc \ "updatedAt" \ "$date").asOpt[Long].getOrElse(0L) / 1000

    Transaction(trxId, expiration, refBlockPrefix, numActions, pending,
      createdAt, blockId, updatedAt)
  }

  def getTransactions(page: Int, size: Int): Future[Seq[Transaction]] = {
    transactionsCollection.flatMap(_.find(
      selector = Json.obj(/* Using Play JSON */),
//      projection = Option.empty[JsObject])
      projection = Some(Json.obj("trx_id" -> 1, "expiration" -> 1,
        "ref_block_prefix" -> 1, "actions" -> 1, "transaction_extensions" -> 1,
        "scheduled" -> 1, "createdAt" -> 1, "block_num" -> 1, "updatedAt" -> 1)))
      .sort(Json.obj("block_id" -> -1))
      .skip(size*(page-1))
      .cursor[JsObject](ReadPreference.primary)
      .collect[Seq](size, Cursor.FailOnError[Seq[JsObject]]())
      .map(_.map(_toTransaction(None, _)))
    )
  }

  def getTransactionById(id: String): Future[Option[Transaction]] = {
    transactionsCollection.flatMap(_.find(
      selector = Json.obj("trx_id" -> id),
//      projection = Option.empty[JsObject])
      projection = Some(Json.obj("expiration" -> 1,
        "ref_block_prefix" -> 1, "actions" -> 1, "transaction_extensions" -> 1,
        "scheduled" -> 1, "createdAt" -> 1, "block_num" -> 1, "updatedAt" -> 1)))
      .one[JsObject]).map(_.map(_toTransaction(Some(id), _)))
  }

  def getTransactionsForBlock(blockNumber: Long, page: Int, size: Int): Future[Seq[Transaction]] = {
    transactionsCollection.flatMap(_.find(
      selector = Json.obj("block_number" -> blockNumber),
//      projection = Option.empty[JsObject])
      projection = Some(Json.obj("trx_id" -> 1, "expiration" -> 1,
        "ref_block_prefix" -> 1, "actions" -> 1, "transaction_extensions" -> 1,
        "scheduled" -> 1, "createdAt" -> 1, "block_num" -> 1, "updatedAt" -> 1)))
      .skip(size*(page-1))
      .cursor[JsObject](ReadPreference.primary)
      .collect[Seq](size, Cursor.FailOnError[Seq[JsObject]]())
      .map(_.map(_toTransaction(None, _)))
    )
  }

}

/**
  * Document sample in 'transactions' collection
{
   "_id":{
      "$oid":"5b84b92c58c9e69f9415cfa4"
   },
   "trx_id":"1d978944393eb95518cc883b7e2d303c57abcb50ad8383da0a1dc528152bbea3",
   "expiration":"2018-08-28T02:54:02",
   "ref_block_num":424,
   "ref_block_prefix":3566143501,
   "max_net_usage_words":0,
   "max_cpu_usage_ms":0,
   "delay_sec":0,
   "context_free_actions":[

   ],
   "actions":[
      {
         "account":"yx.ntoken",
         "name":"transfer",
         "authorization":[
            {
               "actor":"useraccount3",
               "permission":"active"
            }
         ],
         "data":{
            "from":"useraccount3",
            "to":"useraccount2",
            "amount":"5000.0000 DKRW",
            "memo":"memo3"
         },
         "hex_data":"30f2d414217315d620f2d414217315d680f0fa020000000004444b5257000000056d656d6f33"
      }
   ],
   "transaction_extensions":[
      [
         1001,
         "00000357219de8ad"
      ]
   ],
   "signatures":[
      "YSG_K1_KibY1uVsdk8NHxwREwDofZfTeuiBt9nEY2yddAzngW5u8wr6BNjUCBA8gDfhoQ14MDqFtqfHxn75YLB6sN8ppkeuCVJfpr"
   ],
   "context_free_data":[

   ],
   "signing_keys":{
      "0":"YOS5ubmvsnjHviACtfc9SwGbY7SprqTu1P5GQeDfwfZmhCq6aR7GH"
   },
   "accepted":true,
   "implicit":false,
   "scheduled":false,
   "createdAt":{
      "$date":1535424812040
   },
   "irreversible":true,
   "block_id":"000001ea206b2d7cb238743884e0ef16f3e1ee86e620badc98d904319f5787b0",
   "block_num":490,
   "updatedAt":{
      "$date":1535424849005
   }
}
  */