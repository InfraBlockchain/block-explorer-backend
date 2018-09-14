package repositories

import javax.inject.Inject
import models.Block
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
class BlockRepository @Inject()(implicit ec: ExecutionContext, reactiveMongoApi: ReactiveMongoApi){

  import models.BlockJsonFormats._

  def blocksCollection: Future[JSONCollection] = reactiveMongoApi.database.map(_.collection("blocks"))

  def _toBlock(blockIdOpt: Option[String], blockNumOpt: Option[Long], jsDoc: JsObject): Block = {
//    Logger.warn(doc.toString())

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
    val timestamp : Long = DateTime.parse(tsStr + "Z").toInstant.getMillis
    val producer = (jsBlock \ "producer").as[String]
    val confirmed = (jsBlock \ "confirmed").as[Long]
    val prevBlockId = (jsBlock \ "previous").as[String]
    val transactionMerkleRoot = (jsBlock \ "transaction_mroot").as[String]
    val actionMerkleRoot = (jsBlock \ "action_mroot").as[String]
    val version = (jsBlock \ "schedule_version").as[Int]
    val newProducersOpt = (jsBlock \ "new_producers").asOpt[JsValue]
    val numTransactions = (jsBlock \ "transactions").asOpt[JsArray].getOrElse(JsArray()).value.size

    var trxVotes : Long = 0L
    val trxVotesOpt = (jsDoc \ "trx_votes").asOpt[JsArray]
    if (trxVotesOpt.isDefined && trxVotesOpt.get.value.size > 0) {
      trxVotesOpt.get.value.foreach{ trxVoteJs =>
        val trxVoteItems = trxVoteJs.as[JsArray].value
        trxVotes += trxVoteItems(1).as[Long]
      }
    }

    val irreversibleAtOpt = (jsDoc \ "irrAt").asOpt[JsValue].map{ jsVal => (jsVal \ "$date").asOpt[Long].getOrElse(0L) }

    Block(blockId, blockNumber, timestamp, producer, confirmed, prevBlockId,
      transactionMerkleRoot, actionMerkleRoot,
      version, newProducersOpt, numTransactions, trxVotes, (irreversibleAtOpt.getOrElse(0L) > 0L))
  }

  def getBlocks(page: Int, size: Int): Future[Seq[Block]] = {
    blocksCollection.flatMap(_.find(
      selector = Json.obj(/* Using Play JSON */),
      projection = Some(Json.obj("block_id" -> 1, "block_num" -> 1, "block" -> 1, "trx_votes" -> 1, "irrAt" -> 1)))
      .sort(Json.obj("block_num" -> -1))
      .skip(size*(page-1))
      .cursor[JsObject](ReadPreference.primary)
      .collect[Seq](size, Cursor.FailOnError[Seq[JsObject]]())
      .map(_.map(_toBlock(None, None, _)))
    )
  }

//  def getBlockById(id: String): Future[Option[Block]] = {
//    blocksCollection.flatMap(_.find(
//      selector = Json.obj("block_id" -> id),
//      projection = Some(Json.obj("block_num" -> 1, "block" -> 1, "irrAt" -> 1)))
//      .one[JsObject]).map(_.map(_toBlock(Some(id), None, _)))
//  }

  // provide raw block data
  def getBlockById(id: String): Future[Option[JsObject]] = {
    blocksCollection.flatMap(_.find(
      selector = Json.obj("block_id" -> id),
      projection = Option.empty[JsObject])
      .one[JsObject])
      .map( _.map( _ - "_id") )
  }

//  def getBlockByBlockNum(blockNum: Long): Future[Option[Block]] = {
//    blocksCollection.flatMap(_.find(
//      selector = Json.obj("block_num" -> blockNum),
//      projection = Some(Json.obj("block_id" -> 1, "block" -> 1, "irrAt" -> 1)))
//      .one[JsObject]).map(_.map(_toBlock(None, Some(blockNum), _)))
//  }

  // provide raw block data
  def getBlockByBlockNum(blockNum: Long): Future[Option[JsObject]] = {
    blocksCollection.flatMap(_.find(
      selector = Json.obj("block_num" -> blockNum),
      projection = Option.empty[JsObject])
      .one[JsObject])
      .map( _.map( _ - "_id") )
  }

}

/**
  * Document sample in 'blocks' collection
{
   "_id":ObjectId("5b8d064ed76fe49a9a2f9424"),
   "block_id":"000001e83cb1a3965f2bfd298f23a2f9a6b0e545eed57068e674c67bf9c797a2",
   "block_num":488,
   "block":{
      "timestamp":"2018-09-03T10:00:46.000",
      "producer":"producer.c",
      "confirmed":0,
      "previous":"000001e730620c95df7b15e20bfa3d1afcf5bd30021171bbf0d7e26dd96cc845",
      "transaction_mroot":"eabf811ee217c9f82efc793bb0e2658a4af26952852102a9cb8d6bf298974b76",
      "action_mroot":"9712392101a3ac6ee5d770528f37fd469108094ddaea66c2a5683105bd881142",
      "schedule_version":1,
      "new_producers":null,
      "header_extensions":[

      ],
      "producer_signature":"YSG_K1_JwNVLEMgjiFzdQyLzfmttP5v4Sm5E4SjuCqGo3BQXXH8KkJmBkzmTfJwt4eA5Z3mEwbNQapdjbuukr1fquF6GnG1HfYFXR",
      "transactions":[
         {
            "status":"executed",
            "cpu_usage_us":6652,
            "net_usage_words":18,
            "trx":{
               "id":"5c6699dd6c2ab4a258daa41cf885f22a93bc7b17c6c4623e342c701d93fb6a75",
               "signatures":[
                  "YSG_K1_K4dvtHZ4qVjHYHbtJoXKXJQpPVZDDtapW5FbFyrE273VK9JBhJXSt1rkhHCt7Bcaogw6b4ZmzK71A73QAUNTZc7vzZGtGL"
               ],
               "compression":"none",
               "packed_context_free_data":"",
               "context_free_data":[

               ],
               "packed_trx":"6b068d5ba301cd655b3a00000000010000980ad23c41f7000000572d3ccdcd0130f2d414217315d600000000a8ed32322630f2d414217315d620f2d414217315d680f0fa020000000004444b5257000000056d656d6f3101e9030800c00257219de8ad",
               "transaction":{
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
                  ]
               }
            }
         },
         {
            "status":"executed",
            "cpu_usage_us":6075,
            "net_usage_words":18,
            "trx":{
               "id":"13aa8b22c43ea4d734b6c2a8da90e65568a7f9ccf0859423b7d357c743cfa96b",
               "signatures":[
                  "YSG_K1_KA3Sm2qDg1TueY8ojymcMJkMJNgVX9oQNvDCNkgs78W7D4oC9Rd8xGwG7nMiPkMBu8Pm7Jaang8GVtVS98EpZmpMJBbqpy"
               ],
               "compression":"none",
               "packed_context_free_data":"",
               "context_free_data":[

               ],
               "packed_trx":"6b068d5ba301cd655b3a00000000010000980ad23c41f7000000572d3ccdcd0130f2d414217315d600000000a8ed32322630f2d414217315d620f2d414217315d680f0fa020000000004444b5257000000056d656d6f3201e9030800000357219de8ad",
               "transaction":{
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
         },
         {
            "status":"executed",
            "cpu_usage_us":5972,
            "net_usage_words":18,
            "trx":{
               "id":"e32741f8ccd2486d7f6038258351e5a69b9b4aed02d1766289d47e44823ded73",
               "signatures":[
                  "YSG_K1_Kbgme7qd87qRhcnT2LAmCG9K8YJX7Tv9PHeDU3PxzayvJxVDjrfJnsAvcNNorrvK57yhTEjtfzZwVk2dzBVZAEkVvCLvCz"
               ],
               "compression":"none",
               "packed_context_free_data":"",
               "context_free_data":[

               ],
               "packed_trx":"6b068d5ba301cd655b3a00000000010000980ad23c41f7000000572d3ccdcd0130f2d414217315d600000000a8ed32322630f2d414217315d620f2d414217315d680f0fa020000000004444b5257000000056d656d6f3301e9030800000357219de8ad",
               "transaction":{
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
                  ]
               }
            }
         }
      ],
      "block_extensions":[

      ]
   },
   "trx_votes":[
      [
         "producer.f",
         1000000
      ],
      [
         "producer.g",
         2000000
      ]
   ],
   "irrAt":   ISODate("2018-09-03T10:01:22.505   Z"),
   "validated":true,
   "in_current_chain":true
}
  */



/**
  * Document sample in 'block_states' collection
{
   "_id":ObjectId("5b8d064ed76fe49a9a2f9423"),
   "block_id":"000001e83cb1a3965f2bfd298f23a2f9a6b0e545eed57068e674c67bf9c797a2",
   "block_num":488,
   "validated":true,
   "in_current_chain":true,
   "block_header_state":{
      "id":"000001e83cb1a3965f2bfd298f23a2f9a6b0e545eed57068e674c67bf9c797a2",
      "block_num":488,
      "header":{
         "timestamp":"2018-09-03T10:00:46.000",
         "producer":"producer.c",
         "confirmed":0,
         "previous":"000001e730620c95df7b15e20bfa3d1afcf5bd30021171bbf0d7e26dd96cc845",
         "transaction_mroot":"eabf811ee217c9f82efc793bb0e2658a4af26952852102a9cb8d6bf298974b76",
         "action_mroot":"9712392101a3ac6ee5d770528f37fd469108094ddaea66c2a5683105bd881142",
         "schedule_version":1,
         "header_extensions":[

         ],
         "producer_signature":"YSG_K1_JwNVLEMgjiFzdQyLzfmttP5v4Sm5E4SjuCqGo3BQXXH8KkJmBkzmTfJwt4eA5Z3mEwbNQapdjbuukr1fquF6GnG1HfYFXR"
      },
      "dpos_proposed_irreversible_blocknum":455,
      "dpos_irreversible_blocknum":419,
      "bft_irreversible_blocknum":0,
      "pending_schedule_lib_num":166,
      "pending_schedule_hash":"9ce96a594d6ca3074073b2e004dd41ae323323fa442710d9ef909421a7ad7756",
      "pending_schedule":{
         "version":1,
         "producers":[

         ]
      },
      "active_schedule":{
         "version":1,
         "producers":[
            {
               "producer_name":"producer.a",
               "block_signing_key":"YOS5Audoa4mpZaYhp7vwYVCUnsQCUVifftdPipvkfZ9qVggoYoHUn"
            },
            {
               "producer_name":"producer.b",
               "block_signing_key":"YOS5aw9PzjxJCTi23FWtcB6Q8feMhfLg7Toh7PwGoWge4K4xNWQdm"
            },
            {
               "producer_name":"producer.c",
               "block_signing_key":"YOS8cvC5FJozTTVfUVXZ4E4kz1eNsKoBsnG7J76Fw1gX1wstGoUWo"
            },
            {
               "producer_name":"producer.d",
               "block_signing_key":"YOS6ig1G6hpk1Tzj1Ko8zfysATY4eqpb9znyEnx25zbkHscV6qHvy"
            },
            {
               "producer_name":"producer.e",
               "block_signing_key":"YOS72LDKqDc2KvyN1XeEYhv7AbkMUYB8B3fJ55yMn4ZqLzeqxz3w1"
            }
         ]
      },
      "blockroot_merkle":{
         "_active_nodes":[
            "000001e730620c95df7b15e20bfa3d1afcf5bd30021171bbf0d7e26dd96cc845",
            "fa8e754c0dcbec32bb8232c86cd10860491eb7d68e5a152baf918dcf13d54b00",
            "9e5057ab1f4441be470c8600505b386803650ca34db89136c1fce7f5cda04279",
            "69754fe9a31b88b3cea4b15e7091c7cf6814e5edb7f18b512a5e8fccf4b71677",
            "fff53464a4c79cd15568bccce10f6070956cbfa8e31ac74485e7fc16619972f1",
            "7665622c4d58880fbd1a541555fbd4ba738007790c0e73626ecda4849263de98",
            "df09728b951f3955ee3fd7fab63c95391d4838ac57521bc26f0200827dfa4062",
            "3e90f943136eb7a23a3ee101d1b4cd1eb6562c28586c009f2a890075b8d0cf1d"
         ],
         "_node_count":487
      },
      "producer_to_last_produced":[
         [
            "producer.a",
            467
         ],
         [
            "producer.b",
            479
         ],
         [
            "producer.c",
            488
         ],
         [
            "producer.d",
            443
         ],
         [
            "producer.e",
            455
         ],
         [
            "yosemite",
            167
         ]
      ],
      "producer_to_last_implied_irb":[
         [
            "producer.a",
            431
         ],
         [
            "producer.b",
            443
         ],
         [
            "producer.c",
            455
         ],
         [
            "producer.d",
            407
         ],
         [
            "producer.e",
            419
         ]
      ],
      "block_signing_key":"YOS8cvC5FJozTTVfUVXZ4E4kz1eNsKoBsnG7J76Fw1gX1wstGoUWo",
      "confirm_count":[
         1,
         1,
         1,
         1,
         1,
         1,
         1,
         1,
         1,
         1,
         1,
         1,
         2,
         2,
         2,
         2,
         2,
         2,
         2,
         2,
         2,
         2,
         2,
         2,
         3,
         3,
         3,
         3,
         3,
         3,
         3,
         3,
         3
      ],
      "confirmations":[

      ]
   }
}
  */