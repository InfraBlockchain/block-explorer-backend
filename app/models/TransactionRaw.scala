package models

import play.api.libs.json.{JsObject, JsValue}

/**
  * Created by bezalel on 10/09/2018.
  */
case class TransactionRaw(id: String,
                          block_num: Int,
                          block_time: String,
                          producer_block_id: Option[String],
                          receipt: TransactionReceiptHeader,
                          elapsed: Long,
                          net_usage: Long,
                          scheduled: Boolean,
                          action_traces: Seq[ActionTrace],
                          trx_vote: Option[TransactionVote],
                          except: JsValue,
                          BN: Option[Int],
                          expiration: String,
                          ref_block_num: Short,
                          ref_block_prefix: Int,
                          max_cpu_usage_ms: Byte,
                          delay_sec: Int,
                          context_free_actions: Option[Seq[ActionRaw]],
                          actions: Seq[ActionRaw],
                          transaction_extensions: Option[Seq[Seq[JsValue]]],
                          signatures: Seq[String],
                          context_free_data: Option[Seq[String]],
                          signing_keys: JsObject,
                          accepted: Boolean,
                          implicit_ : Boolean, // 'implicit' is not allowed as variable name in scala code
                          block_id: Option[String],
                          irrAt: Option[BSONDate])

case class TransactionReceiptHeader(status: String,
                                    cpu_usage_us: Int,
                                    net_usage_words: Int)

case class TransactionVote(to: String,
                           amt: Int)

case class TransactionReceipt(status: String,
                              cpu_usage_us: Int,
                              net_usage_words: Int,
                              trx: PackedTransaction)

case class PackedTransaction(id: Option[String],
                             signatures: Seq[String],
                             compression: String,
                             packed_context_free_data: String,
                             context_free_data: Option[Seq[String]],
                             packed_trx: String,
                             transaction: TransactionBase)

case class TransactionBase(expiration: String,
                           ref_block_num: Short,
                           ref_block_prefix: Int,
                           max_cpu_usage_ms: Byte,
                           delay_sec: Int,
                           context_free_actions: Option[Seq[ActionRaw]],
                           actions: Seq[ActionRaw],
                           transaction_extensions: Option[Seq[Seq[JsValue]]])
