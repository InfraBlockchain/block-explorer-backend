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
    val timestamp : Long = DateTime.parse(tsStr).toInstant.getMillis
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
      .map (_.map(_toBlock(None, None, _)))
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
