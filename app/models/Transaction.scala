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
import reactivemongo.bson.BSONObjectID

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

  def transactionsCollection: Future[JSONCollection] = reactiveMongoApi.database.map(_.collection("transactions"))
  def transactionTracesCollection: Future[JSONCollection] = reactiveMongoApi.database.map(_.collection("transaction_traces"))

  def _toTransaction(trxIdOpt: Option[String], doc: JsObject): Transaction = {
    Logger.warn(doc.toString())

    val jsDoc = Json.toJson(doc)

    val trxId: String = trxIdOpt match {
      case Some(tid) => tid
      case _ => (jsDoc \ "trx_id").as[String]
    }

    val expirationStr = (jsDoc \ "expiration").as[String]
    val expiration : Long = DateTime.parse(expirationStr).toInstant.getMillis / 1000
    val refBlockPrefix = (jsDoc \ "ref_block_prefix").as[Long]
    val numActions = (jsDoc \ "actions").asOpt[JsArray].getOrElse(JsArray()).value.size
    val pending = (jsDoc \ "scheduled").as[Boolean]
//    val blockId = (jsDoc \ "block_id").asOpt[String].getOrElse("")
    val blockId = (jsDoc \ "bNum").asOpt[Long].getOrElse(0L)

    val docId: String = (jsDoc \ "_id" \ "$oid").as[String]
    val createdAt = BSONObjectID.parse(docId).get.timeSecond
//    val createdAt = (jsDoc \ "createdAt" \ "$date").as[Long] / 1000

    val irreversibleAtOpt = (jsDoc \ "irrAt").asOpt[JsValue].map{ jsVal => (jsVal \ "$date").asOpt[Long].getOrElse(0L) }
    val updatedAt = irreversibleAtOpt.getOrElse(0L) / 1000

    Transaction(trxId, expiration, refBlockPrefix, numActions, pending,
      createdAt, blockId, updatedAt)
  }

  def getTransactions(page: Int, size: Int): Future[Seq[Transaction]] = {
    transactionsCollection.flatMap(_.find(
      selector = Json.obj(/* Using Play JSON */),
      projection = Option.empty[JsObject])
//      projection = Some(Json.obj("_id" -> 1, "trx_id" -> 1, "expiration" -> 1,
//        "ref_block_prefix" -> 1, "actions" -> 1, "transaction_extensions" -> 1,
//        "scheduled" -> 1, "bNum" -> 1)))
      .sort(Json.obj("bId" -> -1))
      .skip(size*(page-1))
      .cursor[JsObject](ReadPreference.primary)
      .collect[Seq](size, Cursor.FailOnError[Seq[JsObject]]())
      .map(_.map(_toTransaction(None, _)))
    )
  }

  def getTransactionById(id: String): Future[Option[JsObject]] = {
    for (
      transactionOpt <- transactionsCollection.flatMap(_.find(
        selector = Json.obj("trx_id" -> id),
        projection = Option.empty[JsObject])
        .one[JsObject]);
      transactionTraceOpt <- transactionTracesCollection.flatMap(_.find(
        selector = Json.obj("id" -> id),
        projection = Option.empty[JsObject])
        .one[JsObject])
    ) yield {
      if (transactionOpt.isDefined && transactionTraceOpt.isDefined) {
         val trxJs = transactionOpt.get.deepMerge(transactionTraceOpt.get) - "_id" - "id"
        Some(trxJs)
      } else {
        None
      }
    }
  }

  def getTransactionsForBlock(blockNumber: Long, page: Int, size: Int): Future[Seq[Transaction]] = {
    transactionsCollection.flatMap(_.find(
      selector = Json.obj("block_number" -> blockNumber),
//      projection = Option.empty[JsObject])
      projection = Some(Json.obj("_id" -> 1, "trx_id" -> 1, "expiration" -> 1,
        "ref_block_prefix" -> 1, "actions" -> 1, "transaction_extensions" -> 1,
        "scheduled" -> 1, "bNum" -> 1)))
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
   "_id":ObjectId("5b8d064dd76fe49a9a2f9412"),
   "trx_id":"5c6699dd6c2ab4a258daa41cf885f22a93bc7b17c6c4623e342c701d93fb6a75",
   "expiration":"2018-09-03T10:01:15",
   "ref_block_num":419,
   "ref_block_prefix":979068365,
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
   ],
   "signatures":[
      "YSG_K1_K4dvtHZ4qVjHYHbtJoXKXJQpPVZDDtapW5FbFyrE273VK9JBhJXSt1rkhHCt7Bcaogw6b4ZmzK71A73QAUNTZc7vzZGtGL"
   ],
   "context_free_data":[

   ],
   "signing_keys":{
      "0":"YOS5ubmvsnjHviACtfc9SwGbY7SprqTu1P5GQeDfwfZmhCq6aR7GH"
   },
   "accepted":true,
   "implicit":false,
   "scheduled":false,
   "irrAt":   ISODate("2018-09-03T10:01:22.505   Z"),
   "bId":"000001e83cb1a3965f2bfd298f23a2f9a6b0e545eed57068e674c67bf9c797a2",
   "bNum":488,
   "bTime":   ISODate("2018-09-03T10:00:46   Z")
}
  */

/**
  * Document sample in 'transaction_traces' collection
{
   "_id":ObjectId("5b8d064dda94975eb24333ed"),
   "id":"5c6699dd6c2ab4a258daa41cf885f22a93bc7b17c6c4623e342c701d93fb6a75",
   "receipt":{
      "status":"executed",
      "cpu_usage_us":6652,
      "net_usage_words":18
   },
   "elapsed":6652,
   "net_usage":144,
   "scheduled":false,
   "action_traces":[
      {
         "receipt":{
            "receiver":"yx.ntoken",
            "act_digest":"50cf385834699415c233f608b6a7be0e3254767189144b26142bbb641aa52ab7",
            "global_sequence":644,
            "recv_sequence":20,
            "auth_sequence":[
               [
                  "useraccount3",
                  5
               ]
            ],
            "code_sequence":1,
            "abi_sequence":1
         },
         "act":{
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
         },
         "elapsed":2914,
         "cpu_usage":0,
         "console":"",
         "total_cpu_usage":0,
         "trx_id":"5c6699dd6c2ab4a258daa41cf885f22a93bc7b17c6c4623e342c701d93fb6a75",
         "inline_traces":[
            {
               "receipt":{
                  "receiver":"yx.ntoken",
                  "act_digest":"cefd67ce9fbe5d07628c1f8e10f041f0984a77eaf1e406794bf9512a1ed3beac",
                  "global_sequence":645,
                  "recv_sequence":21,
                  "auth_sequence":[
                     [
                        "useraccount3",
                        6
                     ],
                     [
                        "yosemite",
                        595
                     ]
                  ],
                  "code_sequence":1,
                  "abi_sequence":1
               },
               "act":{
                  "account":"yx.ntoken",
                  "name":"ntransfer",
                  "authorization":[
                     {
                        "actor":"useraccount3",
                        "permission":"active"
                     },
                     {
                        "actor":"yosemite",
                        "permission":"active"
                     }
                  ],
                  "data":{
                     "from":"useraccount3",
                     "to":"useraccount2",
                     "token":{
                        "amount":"5000.0000 DKRW",
                        "issuer":"sysdepo1"
                     },
                     "memo":"memo1"
                  },
                  "hex_data":"30f2d414217315d620f2d414217315d680f0fa020000000004444b5257000000000000815695b0c7056d656d6f31"
               },
               "elapsed":2203,
               "cpu_usage":0,
               "console":"",
               "total_cpu_usage":0,
               "trx_id":"5c6699dd6c2ab4a258daa41cf885f22a93bc7b17c6c4623e342c701d93fb6a75",
               "inline_traces":[
                  {
                     "receipt":{
                        "receiver":"useraccount3",
                        "act_digest":"cefd67ce9fbe5d07628c1f8e10f041f0984a77eaf1e406794bf9512a1ed3beac",
                        "global_sequence":646,
                        "recv_sequence":5,
                        "auth_sequence":[
                           [
                              "useraccount3",
                              7
                           ],
                           [
                              "yosemite",
                              596
                           ]
                        ],
                        "code_sequence":1,
                        "abi_sequence":1
                     },
                     "act":{
                        "account":"yx.ntoken",
                        "name":"ntransfer",
                        "authorization":[
                           {
                              "actor":"useraccount3",
                              "permission":"active"
                           },
                           {
                              "actor":"yosemite",
                              "permission":"active"
                           }
                        ],
                        "data":{
                           "from":"useraccount3",
                           "to":"useraccount2",
                           "token":{
                              "amount":"5000.0000 DKRW",
                              "issuer":"sysdepo1"
                           },
                           "memo":"memo1"
                        },
                        "hex_data":"30f2d414217315d620f2d414217315d680f0fa020000000004444b5257000000000000815695b0c7056d656d6f31"
                     },
                     "elapsed":3,
                     "cpu_usage":0,
                     "console":"",
                     "total_cpu_usage":0,
                     "trx_id":"5c6699dd6c2ab4a258daa41cf885f22a93bc7b17c6c4623e342c701d93fb6a75",
                     "inline_traces":[

                     ]
                  },
                  {
                     "receipt":{
                        "receiver":"useraccount2",
                        "act_digest":"cefd67ce9fbe5d07628c1f8e10f041f0984a77eaf1e406794bf9512a1ed3beac",
                        "global_sequence":647,
                        "recv_sequence":8,
                        "auth_sequence":[
                           [
                              "useraccount3",
                              8
                           ],
                           [
                              "yosemite",
                              597
                           ]
                        ],
                        "code_sequence":1,
                        "abi_sequence":1
                     },
                     "act":{
                        "account":"yx.ntoken",
                        "name":"ntransfer",
                        "authorization":[
                           {
                              "actor":"useraccount3",
                              "permission":"active"
                           },
                           {
                              "actor":"yosemite",
                              "permission":"active"
                           }
                        ],
                        "data":{
                           "from":"useraccount3",
                           "to":"useraccount2",
                           "token":{
                              "amount":"5000.0000 DKRW",
                              "issuer":"sysdepo1"
                           },
                           "memo":"memo1"
                        },
                        "hex_data":"30f2d414217315d620f2d414217315d680f0fa020000000004444b5257000000000000815695b0c7056d656d6f31"
                     },
                     "elapsed":3,
                     "cpu_usage":0,
                     "console":"",
                     "total_cpu_usage":0,
                     "trx_id":"5c6699dd6c2ab4a258daa41cf885f22a93bc7b17c6c4623e342c701d93fb6a75",
                     "inline_traces":[

                     ]
                  }
               ]
            },
            {
               "receipt":{
                  "receiver":"yx.ntoken",
                  "act_digest":"4d64896a95e1e16e171a4e9f6e3803da13a172d9dd8356ef80ca7a11433c364f",
                  "global_sequence":648,
                  "recv_sequence":22,
                  "auth_sequence":[
                     [
                        "useraccount3",
                        9
                     ],
                     [
                        "yosemite",
                        598
                     ]
                  ],
                  "code_sequence":1,
                  "abi_sequence":1
               },
               "act":{
                  "account":"yx.ntoken",
                  "name":"payfee",
                  "authorization":[
                     {
                        "actor":"useraccount3",
                        "permission":"active"
                     },
                     {
                        "actor":"yosemite",
                        "permission":"active"
                     }
                  ],
                  "data":{
                     "payer":"useraccount3",
                     "token":{
                        "amount":"100.0000 DKRW",
                        "issuer":"sysdepo1"
                     }
                  },
                  "hex_data":"30f2d414217315d640420f000000000004444b5257000000000000815695b0c7"
               },
               "elapsed":1299,
               "cpu_usage":0,
               "console":"",
               "total_cpu_usage":0,
               "trx_id":"5c6699dd6c2ab4a258daa41cf885f22a93bc7b17c6c4623e342c701d93fb6a75",
               "inline_traces":[
                  {
                     "receipt":{
                        "receiver":"useraccount3",
                        "act_digest":"4d64896a95e1e16e171a4e9f6e3803da13a172d9dd8356ef80ca7a11433c364f",
                        "global_sequence":649,
                        "recv_sequence":6,
                        "auth_sequence":[
                           [
                              "useraccount3",
                              10
                           ],
                           [
                              "yosemite",
                              599
                           ]
                        ],
                        "code_sequence":1,
                        "abi_sequence":1
                     },
                     "act":{
                        "account":"yx.ntoken",
                        "name":"payfee",
                        "authorization":[
                           {
                              "actor":"useraccount3",
                              "permission":"active"
                           },
                           {
                              "actor":"yosemite",
                              "permission":"active"
                           }
                        ],
                        "data":{
                           "payer":"useraccount3",
                           "token":{
                              "amount":"100.0000 DKRW",
                              "issuer":"sysdepo1"
                           }
                        },
                        "hex_data":"30f2d414217315d640420f000000000004444b5257000000000000815695b0c7"
                     },
                     "elapsed":3,
                     "cpu_usage":0,
                     "console":"",
                     "total_cpu_usage":0,
                     "trx_id":"5c6699dd6c2ab4a258daa41cf885f22a93bc7b17c6c4623e342c701d93fb6a75",
                     "inline_traces":[

                     ]
                  },
                  {
                     "receipt":{
                        "receiver":"yx.txfee",
                        "act_digest":"4d64896a95e1e16e171a4e9f6e3803da13a172d9dd8356ef80ca7a11433c364f",
                        "global_sequence":650,
                        "recv_sequence":23,
                        "auth_sequence":[
                           [
                              "useraccount3",
                              11
                           ],
                           [
                              "yosemite",
                              600
                           ]
                        ],
                        "code_sequence":1,
                        "abi_sequence":1
                     },
                     "act":{
                        "account":"yx.ntoken",
                        "name":"payfee",
                        "authorization":[
                           {
                              "actor":"useraccount3",
                              "permission":"active"
                           },
                           {
                              "actor":"yosemite",
                              "permission":"active"
                           }
                        ],
                        "data":{
                           "payer":"useraccount3",
                           "token":{
                              "amount":"100.0000 DKRW",
                              "issuer":"sysdepo1"
                           }
                        },
                        "hex_data":"30f2d414217315d640420f000000000004444b5257000000000000815695b0c7"
                     },
                     "elapsed":50,
                     "cpu_usage":0,
                     "console":"",
                     "total_cpu_usage":0,
                     "trx_id":"5c6699dd6c2ab4a258daa41cf885f22a93bc7b17c6c4623e342c701d93fb6a75",
                     "inline_traces":[

                     ]
                  }
               ]
            }
         ]
      }
   ],
   "trx_vote":{
      "to":"producer.f",
      "amt":1000000
   },
   "except":null
}
  */