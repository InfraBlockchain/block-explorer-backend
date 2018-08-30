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
  * Created by bezalel on 28/08/2018.
  */

case class Block(id: String,
                 blockNumber: Long,
                 timestamp: Long,
                 producer: String,
                 confirmed: Long,
                 prevBlockId: String,
                 transactionMerkleRoot: String,
                 actionMerkleRoot: String,
                 version: Int,
                 newProducers: Option[JsValue],
                 numTransactions: Int,
                 irreversible: Boolean)

object BlockJsonFormats{
  import play.api.libs.json._

  implicit val blockFormat: OFormat[Block] = Json.format[Block]
}

class BlockRepository @Inject()(implicit ec: ExecutionContext, reactiveMongoApi: ReactiveMongoApi){

  import BlockJsonFormats._

  def blocksCollection: Future[JSONCollection] = reactiveMongoApi.database.map(_.collection("blocks"))

//  def bsonToBlock(doc: BSONDocument): Block = {
//    Logger.warn(Json.toJson(doc).toString())
//    val blockId = doc.getAs[BSONString]("block_id").get.value
//    Logger.error(blockId)
//    val blockNumber = doc.getAs[BSONInteger]("block_num").get.value
//    val block = doc.getAs[BSONDocument]("block").get
//    val timestamp : Long = 0L
//    val producer = block.getAs[BSONString]("producer").get.value
//    val confirmed = block.getAs[BSONInteger]("confirmed").get.value
//    val prevBlockId = block.getAs[BSONString]("previous").get.value
//    val transactionMerkleRoot = block.getAs[BSONString]("transaction_mroot").get.value
//    val actionMerkleRoot = block.getAs[BSONString]("action_mroot").get.value
//    val version = block.getAs[BSONInteger]("schedule_version").get.value
//    val newProducers = JsNull
//    val numTransactions = block.getAs[BSONArray]("transactions").get.size
//    val irreversible = doc.getAs[BSONBoolean]("irreversible").getOrElse(BSONBoolean(false)).value
//
//    Block(blockId, blockNumber, timestamp, producer, confirmed, prevBlockId,
//      transactionMerkleRoot, actionMerkleRoot,
//      version, newProducers, numTransactions, irreversible)
//  }

  def _toBlock(blockIdOpt: Option[String], blockNumOpt: Option[Long], doc: JsObject): Block = {
    Logger.warn(doc.toString())
    val jsDoc = Json.toJson(doc)

    val blockId: String = blockIdOpt match {
      case Some(bid) => bid
      case _ => (jsDoc \ "block_id").as[String]
    }
    val blockNumber = blockNumOpt match {
      case Some(bnum) => bnum
      case _ => (jsDoc \ "block_num").as[Long]
    }
    val jsBlock = (jsDoc \ "block").as[JsValue]
    val tsStr = (jsBlock \ "timestamp").as[String]
    val timestamp : Long = DateTime.parse(tsStr).toInstant.getMillis / 1000
    val producer = (jsBlock \ "producer").as[String]
    val confirmed = (jsBlock \ "confirmed").as[Long]
    val prevBlockId = (jsBlock \ "previous").as[String]
    val transactionMerkleRoot = (jsBlock \ "transaction_mroot").as[String]
    val actionMerkleRoot = (jsBlock \ "action_mroot").as[String]
    val version = (jsBlock \ "schedule_version").as[Int]
    val newProducersOpt = (jsBlock \ "new_producers").asOpt[JsValue]
    val numTransactions = (jsBlock \ "transactions").asOpt[JsArray].getOrElse(JsArray()).value.size
    val irreversible = (jsDoc \ "irreversible").asOpt[Boolean].getOrElse(false)

    Block(blockId, blockNumber, timestamp, producer, confirmed, prevBlockId,
      transactionMerkleRoot, actionMerkleRoot,
      version, newProducersOpt, numTransactions, irreversible)
  }

  def getBlocks(page: Int, size: Int): Future[Seq[Block]] = {
    blocksCollection.flatMap(_.find(
      selector = Json.obj(/* Using Play JSON */),
      projection = Some(Json.obj("block_id" -> 1, "block_num" -> 1, "block" -> 1, "irreversible" -> 1)))
      .sort(Json.obj("block_num" -> -1))
      .skip(size*(page-1))
      .cursor[JsObject](ReadPreference.primary)
      .collect[Seq](size, Cursor.FailOnError[Seq[JsObject]]())
      .map(_.map(_toBlock(None, None, _)))
    )
  }

  def getBlockById(id: String): Future[Option[Block]] = {
    blocksCollection.flatMap(_.find(
      selector = Json.obj("block_id" -> id),
      projection = Some(Json.obj("block_num" -> 1, "block" -> 1, "irreversible" -> 1)))
      .one[JsObject]).map(_.map(_toBlock(Some(id), None, _)))
  }

  def getBlockByBlockNum(blockNum: Long): Future[Option[Block]] = {
    blocksCollection.flatMap(_.find(
      selector = Json.obj("block_num" -> blockNum),
      projection = Some(Json.obj("block_id" -> 1, "block" -> 1, "irreversible" -> 1)))
      .one[JsObject]).map(_.map(_toBlock(None, Some(blockNum), _)))
  }

}

/**
  * Document sample in 'blocks' collection
{
   "_id":{
      "$oid":"5b84b92c58c9e69f9415cfa2"
   },
   "block_num":489,
   "block":{
      "timestamp":"2018-08-28T02:53:32.000",
      "producer":"producer.a",
      "confirmed":0,
      "previous":"000001e83ebea3dbbbaa1ca11d17111925aec902df6249a245d3a5fbb3394a7a",
      "transaction_mroot":"9bdfa62757f21885ceff4c9aa3a7bb6e5472e6df4cfea4a00995aea830867886",
      "action_mroot":"010d9bc25150629260d74e5720ec95c3067f6133e06394ca64dda2aaa653fb9c",
      "schedule_version":1,
      "new_producers":null,
      "header_extensions":[

      ],
      "producer_signature":"YSG_K1_K1i2amxoABEUzY8qM4L8kx8sgA7ENPqMkQJh9RSfgt3ESnWcbbrohUQ2Mup8gkauY36FBaWfviJJw7v5yf4dNV61k4Aozc",
      "transactions":[
         {
            "status":"executed",
            "cpu_usage_us":10302,
            "net_usage_words":18,
            "trx":{
               "id":"d718baea3e49f390dd781ed976ef037a93ac55e4f46bf9e51403445172fbd5ba",
               "signatures":[
                  "YSG_K1_Kfb2KDLZnV5X6YURjSDR5NucRyKdXmCTukGXSteCLtuLHPTMeM97Xroe2o8qF2rxX3PgZBxQieDZkFUk1tMqr5Lkw4jqYr"
               ],
               "compression":"none",
               "packed_context_free_data":"",
               "context_free_data":[

               ],
               "packed_trx":"49b9845ba8010d088fd400000000010000980ad23c41f7000000572d3ccdcd0130f2d414217315d600000000a8ed32322630f2d414217315d620f2d414217315d680f0fa020000000004444b5257000000056d656d6f3101e9030800c00257219de8ad",
               "transaction":{
                  "expiration":"2018-08-28T02:54:01",
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
                           "memo":"memo1"
                        },
                        "hex_data":"30f2d414217315d620f2d414217315d680f0fa020000000004444b5257000000056d656d6f31"
                     }
                  ],
                  "transaction_extensions":[
                     [
                        1001,
                        "00c00257219de8ad"
                     ]
                  ]
               }
            }
         },
         {
            "status":"executed",
            "cpu_usage_us":10599,
            "net_usage_words":18,
            "trx":{
               "id":"67fa12029be9b3aeb445c2374b1cf96ffba1d41f42bfe1eaa4e21b45923c520e",
               "signatures":[
                  "YSG_K1_KkwHRc2WFbbSEkhmeH8BUtKY26RP4KzCqT7xCzNe1qNQzXCr1KfvGGVaQ5hz3M2WeRdVQDgCZSycHjpQEd8BXvAR2ejJJ9"
               ],
               "compression":"none",
               "packed_context_free_data":"",
               "context_free_data":[

               ],
               "packed_trx":"49b9845ba8010d088fd400000000010000980ad23c41f7000000572d3ccdcd0130f2d414217315d600000000a8ed32322630f2d414217315d620f2d414217315d680f0fa020000000004444b5257000000056d656d6f3201e9030800000357219de8ad",
               "transaction":{
                  "expiration":"2018-08-28T02:54:01",
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
                           "memo":"memo2"
                        },
                        "hex_data":"30f2d414217315d620f2d414217315d680f0fa020000000004444b5257000000056d656d6f32"
                     }
                  ],
                  "transaction_extensions":[
                     [
                        1001,
                        "00000357219de8ad"
                     ]
                  ]
               }
            }
         }
      ],
      "block_extensions":[

      ]
   },
   "irreversible":true
}
  */
