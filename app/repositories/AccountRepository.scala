package repositories

import javax.inject.Inject
import models.{Account, AccountPermission}
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.{Cursor, ReadPreference}
import reactivemongo.play.json._
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by bezalel on 14/09/2018.
  */
class AccountRepository @Inject() (implicit ec: ExecutionContext, reactiveMongoApi: ReactiveMongoApi) {

  import models.AccountJsonFormats._

  def accountsCollection: Future[JSONCollection] = reactiveMongoApi.database.map(_.collection("accounts"))
  def publicKeysCollection: Future[JSONCollection] = reactiveMongoApi.database.map(_.collection("pub_keys"))

  def _toAccount(jsDoc: JsObject): Account = {
    //    Logger.warn(doc.toString())

    val name = (jsDoc \ "name").as[String]
    val createdAt = (jsDoc \ "createdAt" \ "$date").as[Long]
    val updatedAtOpt= (jsDoc \ "updatedAt").asOpt[JsValue].map{ jsVal => (jsVal \ "$date").asOpt[Long].getOrElse(0L) }
    val abiJsOpt = (jsDoc \ "abi").asOpt[JsValue]

    Account(name, createdAt, updatedAtOpt, abiJsOpt)
  }

  def getAccountByName(name: String): Future[Option[Account]] = {
    accountsCollection.flatMap(_.find(
      selector = Json.obj("name" -> name),
      projection = Option.empty[JsObject])
      .one[JsObject])
      .map( _.map(_toAccount(_)) )
  }

  def _toAccountPermission(jsDoc: JsObject): AccountPermission = {
    //    Logger.warn(doc.toString())

    val publicKey = (jsDoc \ "public_key").as[String]
    val account = (jsDoc \ "account").as[String]
    val permission = (jsDoc \ "permission").as[String]
    val createdAt = (jsDoc \ "createdAt" \ "$date").as[Long]

    AccountPermission(account, permission, publicKey, createdAt)
  }

  def getAccountPermissionsByPubKey(publicKey: String): Future[Seq[AccountPermission]] = {
    publicKeysCollection.flatMap(_.find(
      selector = Json.obj("public_key" -> publicKey),
      projection = Option.empty[JsObject])
      .cursor[JsObject](ReadPreference.primary)
      .collect[Seq](100, Cursor.FailOnError[Seq[JsObject]]())
      .map(_.map(_toAccountPermission(_)))
    )
  }
}

/**
  * Document sample in 'accounts' collection
{
   "_id":ObjectId("5b97605f2178c50c5f8c697a"),
   "name":"yx.ntoken",
   "createdAt":   ISODate("2018-09-11T06:27:43.114   Z"),
   "abi":{
      "version":"eosio::abi/1.0",
      "types":[
         {
            "new_type_name":"account_name",
            "type":"name"
         }
      ],
      "structs":[
         {
            "name":"yx_asset",
            "base":"",
            "fields":[
               {
                  "name":"amount",
                  "type":"asset"
               },
               {
                  "name":"issuer",
                  "type":"account_name"
               }
            ]
         },
         {
            "name":"nissue",
            "base":"",
            "fields":[
               {
                  "name":"to",
                  "type":"account_name"
               },
               {
                  "name":"token",
                  "type":"yx_asset"
               },
               {
                  "name":"memo",
                  "type":"string"
               }
            ]
         },
         {
            "name":"nredeem",
            "base":"",
            "fields":[
               {
                  "name":"token",
                  "type":"yx_asset"
               },
               {
                  "name":"memo",
                  "type":"string"
               }
            ]
         },
         {
            "name":"transfer",
            "base":"",
            "fields":[
               {
                  "name":"from",
                  "type":"account_name"
               },
               {
                  "name":"to",
                  "type":"account_name"
               },
               {
                  "name":"amount",
                  "type":"asset"
               },
               {
                  "name":"memo",
                  "type":"string"
               }
            ]
         },
         {
            "name":"wptransfer",
            "base":"",
            "fields":[
               {
                  "name":"from",
                  "type":"account_name"
               },
               {
                  "name":"to",
                  "type":"account_name"
               },
               {
                  "name":"amount",
                  "type":"asset"
               },
               {
                  "name":"payer",
                  "type":"account_name"
               },
               {
                  "name":"memo",
                  "type":"string"
               }
            ]
         },
         {
            "name":"ntransfer",
            "base":"",
            "fields":[
               {
                  "name":"from",
                  "type":"account_name"
               },
               {
                  "name":"to",
                  "type":"account_name"
               },
               {
                  "name":"token",
                  "type":"yx_asset"
               },
               {
                  "name":"memo",
                  "type":"string"
               }
            ]
         },
         {
            "name":"wpntransfer",
            "base":"",
            "fields":[
               {
                  "name":"from",
                  "type":"account_name"
               },
               {
                  "name":"to",
                  "type":"account_name"
               },
               {
                  "name":"token",
                  "type":"yx_asset"
               },
               {
                  "name":"payer",
                  "type":"account_name"
               },
               {
                  "name":"memo",
                  "type":"string"
               }
            ]
         },
         {
            "name":"payfee",
            "base":"",
            "fields":[
               {
                  "name":"payer",
                  "type":"account_name"
               },
               {
                  "name":"token",
                  "type":"yx_asset"
               }
            ]
         },
         {
            "name":"setkycrule",
            "base":"",
            "fields":[
               {
                  "name":"type",
                  "type":"uint8"
               },
               {
                  "name":"kyc",
                  "type":"uint16"
               }
            ]
         },
         {
            "name":"account_total_type",
            "base":"",
            "fields":[
               {
                  "name":"amount",
                  "type":"asset"
               }
            ]
         },
         {
            "name":"account_type",
            "base":"",
            "fields":[
               {
                  "name":"token",
                  "type":"yx_asset"
               }
            ]
         },
         {
            "name":"stats_type",
            "base":"",
            "fields":[
               {
                  "name":"key",
                  "type":"name"
               },
               {
                  "name":"supply",
                  "type":"asset"
               },
               {
                  "name":"options",
                  "type":"uint8"
               }
            ]
         },
         {
            "name":"kycrule_type",
            "base":"",
            "fields":[
               {
                  "name":"type",
                  "type":"uint8"
               },
               {
                  "name":"kyc",
                  "type":"uint16"
               }
            ]
         }
      ],
      "actions":[
         {
            "name":"nissue",
            "type":"nissue",
            "ricardian_contract":""
         },
         {
            "name":"nredeem",
            "type":"nredeem",
            "ricardian_contract":""
         },
         {
            "name":"transfer",
            "type":"transfer",
            "ricardian_contract":""
         },
         {
            "name":"wptransfer",
            "type":"wptransfer",
            "ricardian_contract":""
         },
         {
            "name":"ntransfer",
            "type":"ntransfer",
            "ricardian_contract":""
         },
         {
            "name":"wpntransfer",
            "type":"wpntransfer",
            "ricardian_contract":""
         },
         {
            "name":"payfee",
            "type":"payfee",
            "ricardian_contract":""
         },
         {
            "name":"setkycrule",
            "type":"setkycrule",
            "ricardian_contract":""
         }
      ],
      "tables":[
         {
            "name":"ntaccounts",
            "index_type":"i64",
            "key_names":[

            ],
            "key_types":[

            ],
            "type":"account_type"
         },
         {
            "name":"ntaccountstt",
            "index_type":"i64",
            "key_names":[

            ],
            "key_types":[

            ],
            "type":"account_total_type"
         },
         {
            "name":"ntstats",
            "index_type":"i64",
            "key_names":[
               "key"
            ],
            "key_types":[
               "uint64"
            ],
            "type":"stats_type"
         },
         {
            "name":"kycrule",
            "index_type":"i64",
            "key_names":[
               "type"
            ],
            "key_types":[
               "uint8"
            ],
            "type":"kycrule_type"
         }
      ],
      "ricardian_clauses":[

      ],
      "error_messages":[

      ],
      "abi_extensions":[

      ]
   },
   "updatedAt":   ISODate("2018-09-11T06:28:06.145   Z")
}
  */

/**
  * Document sample in 'pub_keys' collection
{
   "_id":ObjectId("5b97607e2178c50c5f8c6ae9"),
   "account":"producer.g",
   "permission":"owner",
   "public_key":"YOS5t1fHFunR2rWq5z8NHPrxj1H4xG5Vq4bGKcH33yg1eZMCVPQRq",
   "createdAt":   ISODate("2018-09-11T06:28:14.109   Z")
}
{
   "_id":ObjectId("5b97607e2178c50c5f8c6aea"),
   "account":"producer.g",
   "permission":"active",
   "public_key":"YOS5t1fHFunR2rWq5z8NHPrxj1H4xG5Vq4bGKcH33yg1eZMCVPQRq",
   "createdAt":   ISODate("2018-09-11T06:28:14.109   Z")
}
  */
