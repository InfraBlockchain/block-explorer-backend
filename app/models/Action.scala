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

case class Action(id: String,
                  transaction: String,
                  account: String,
                  name: String,
                  data: JsValue,
                  authorization: JsValue,
                  seq: Long,
                  parentId: String)

object ActionJsonFormats{
  import play.api.libs.json._

  implicit val actionFormat: OFormat[Action] = Json.format[Action]
}

class ActionRepository @Inject()(implicit ec: ExecutionContext, reactiveMongoApi: ReactiveMongoApi){

  import ActionJsonFormats._

  def actionTracesCollection: Future[JSONCollection] = reactiveMongoApi.database.map(_.collection("action_traces"))

  def _toAction(actionIdOpt: Option[String], transactionIdOpt: Option[String],  doc: JsObject): Action = {
//    Logger.warn(doc.toString())

    val jsDoc = Json.toJson(doc)

    val actionId: String = actionIdOpt match {
      case Some(aid) => aid
      case _ => (jsDoc \ "_id" \ "$oid").as[String]
    }

    val transaction: String = transactionIdOpt match {
      case Some(tid) => tid
      case _ => (jsDoc \ "trx_id").as[String]
    }

    val jsAct = (jsDoc \ "act").as[JsValue]
    val account = (jsAct \ "account").as[String]
    val name = (jsAct \ "name").as[String]
    val authorization = (jsAct \ "authorization").as[JsValue]
    val data = (jsAct \ "data").as[JsValue]

    val jsReceipt = (jsDoc \ "receipt").as[JsValue]
    val seq = (jsReceipt \ "global_sequence").as[Long]
    val parentId = ""

    Action(actionId, transaction, account, name, authorization, data, seq, parentId)
  }

  def getActionById(id: String): Future[Option[Action]] = {
    actionTracesCollection.flatMap(_.find(
      selector = Json.obj("_id" -> Json.obj("$oid" -> id)),
//      projection = Option.empty[JsObject])
      projection = Some(Json.obj("receipt" -> 1, "act" -> 1, "trx_id" -> 1)))
      .one[JsObject]).map(_.map(_toAction(Some(id), None, _)))
  }

  def getActionInTransaction(transactionId: String, idx: Int): Future[Option[Action]] = {
    actionTracesCollection.flatMap(_.find(
      selector = Json.obj("trx_id" -> transactionId),
//      projection = Option.empty[JsObject])
      projection = Some(Json.obj("_id" -> 1, "receipt" -> 1, "act" -> 1)))
      .skip(idx)
      .cursor[JsObject](ReadPreference.primary)
      .collect[Seq](1, Cursor.FailOnError[Seq[JsObject]]())
      .map(_.map(_toAction(None, Some(transactionId), _))).map { actSeq =>
        if (actSeq.size > 0) {
          Some(actSeq(0))
        } else {
          None
        }
      }
    )
  }

  def getActionsInTransaction(transactionId: String, page: Int, size: Int): Future[Seq[JsObject]] = {
    actionTracesCollection.flatMap(_.find(
      selector = Json.obj("trx_id" -> transactionId),
      projection = Option.empty[JsObject])
      .skip(size*(page-1))
      .cursor[JsObject](ReadPreference.primary)
      .collect[Seq](size, Cursor.FailOnError[Seq[JsObject]]())
      .map(_.map( _ - "_id"))
    )
  }
}

/**
  * Document sample in 'action_traces' collection
{
   "_id":ObjectId("5b8d064dd76fe49a9a2f940c"),
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
   "bNum":488,
   "bTime":   ISODate("2018-09-03T10:00:46   Z")
}
  */