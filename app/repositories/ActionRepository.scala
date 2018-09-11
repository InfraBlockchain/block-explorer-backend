package repositories

import javax.inject.Inject
import models.Action
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.{Cursor, ReadPreference}
import reactivemongo.play.json._
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by bezalel on 29/08/2018.
  */
class ActionRepository @Inject()(implicit ec: ExecutionContext, reactiveMongoApi: ReactiveMongoApi){

  import models.ActionJsonFormats._

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
    val parent = (jsDoc \ "parent").asOpt[Long].getOrElse(0L)

    Action(actionId, transaction, account, name, authorization, data, seq, parent)
  }

  def getActionById(global_sequence: Long): Future[Option[JsObject]] = {
    actionTracesCollection.flatMap(_.find(
//      selector = Json.obj("_id" -> Json.obj("$oid" -> id)),
      selector = Json.obj("receipt.global_sequence" -> global_sequence),
//      projection = Option.empty[JsObject])
      projection = Some(Json.obj("receipt" -> 1, "act" -> 1, "trx_id" -> 1)))
      .one[JsObject])
//      .map(_.map(_toAction(Some(id), None, _)))
      .map( _.map( _ - "_id") )
  }

  // provides action blockchain raw data
  def getActionInTransaction(transactionId: String, idx: Int): Future[Option[JsObject]] = {
    actionTracesCollection.flatMap(_.find(
      selector = Json.obj("trx_id" -> transactionId),
      projection = Option.empty[JsObject])
      .skip(idx)
      .cursor[JsObject](ReadPreference.primary)
      .collect[Seq](1, Cursor.FailOnError[Seq[JsObject]]())
      .map(_.map( _ - "_id" )).map { actSeq =>
        if (actSeq.size > 0) {
          Some(actSeq(0))
        } else {
          None
        }
      }
    )
  }

  // provides action blockchain raw data
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

  // provides action blockchain raw data
  def getActionsByReceiverAccount(receiverAccount: String, start_seq: Long, offset: Int): Future[Seq[JsObject]] = {
    if (start_seq == -1L) {
      if (offset < 0) {
        actionTracesCollection.flatMap(_.find(
          selector = Json.obj("receipt.receiver" -> receiverAccount),
          projection = Option.empty[JsObject])
          .sort(Json.obj("receipt.recv_sequence" -> -1))
          .cursor[JsObject](ReadPreference.primary)
          .collect[Seq](-offset, Cursor.FailOnError[Seq[JsObject]]())
          .map(_.map( _ - "_id"))
        )
      } else {
        Future.successful(Seq())
      }
    } else {
      if (offset < 0) {
        actionTracesCollection.flatMap(_.find(
          selector = Json.obj("receipt.receiver" -> receiverAccount,
            "receipt.recv_sequence" -> Json.obj("$lt" -> start_seq)),
          projection = Option.empty[JsObject])
          .sort(Json.obj("receipt.recv_sequence" -> -1))
          .cursor[JsObject](ReadPreference.primary)
          .collect[Seq](-offset, Cursor.FailOnError[Seq[JsObject]]())
          .map(_.map( _ - "_id"))
        )
      } else if (offset > 0) {
        actionTracesCollection.flatMap(_.find(
          selector = Json.obj("receipt.receiver" -> receiverAccount,
            "receipt.recv_sequence" -> Json.obj("$gte" -> start_seq)),
          projection = Option.empty[JsObject])
          .sort(Json.obj("receipt.recv_sequence" -> 1))
          .cursor[JsObject](ReadPreference.primary)
          .collect[Seq](offset, Cursor.FailOnError[Seq[JsObject]]())
          .map(_.map( _ - "_id"))
        )
      } else {
        Future.successful(Seq())
      }
    }
  }

}

/**
  * Document sample in 'action_traces' collection
{
   "_id":ObjectId("5b9354812b60c60ac507b9e0"),
   "receipt":{
      "receiver":"yx.ntoken",
      "act_digest":"cefd67ce9fbe5d07628c1f8e10f041f0984a77eaf1e406794bf9512a1ed3beac",
      "global_sequence":649,
      "recv_sequence":21,
      "auth_sequence":[
         [
            "useraccount3",
            6
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
   "elapsed":2803,
   "cpu_usage":0,
   "console":"",
   "total_cpu_usage":0,
   "trx_id":"a1481e23f00a7bdb37cb57ac0f78419b7d5baa0027ce21cd961906bf4425a390",
   "bNum":492,
   "bTime":   ISODate("2018-09-08T04:48:02   Z"),
   "parent":NumberLong(648)
}
  */