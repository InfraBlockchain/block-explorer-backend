package models

import play.api.libs.json.JsValue

/**
  * Created by bezalel on 10/09/2018.
  */
case class BlockRaw(block_id: String,
                    block_num: Int,
                    block: BlockHeader
                   )

case class BlockHeader(timestamp: String,
                       producer: String,
                       confirmed: Short,
                       previous: String,
                       transaction_mroot: String,
                       action_mroot: String,
                       schedule_version: Int,
                       new_producers: Option[ProducerSchedule],
                       header_extensions: Option[Seq[Seq[JsValue]]],
                       producer_signature: String,
                       transactions: Option[Seq[TransactionReceipt]],
                       block_extensions: Option[Seq[Seq[JsValue]]],
                       trx_votes: Option[Seq[Seq[JsValue]]],
                       irrAt: Option[BSONDate],
                       validated: Boolean,
                       in_current_chain: Boolean
                      )

case class ProducerSchedule(version: Int,
                            producers: Seq[ProducerKey]
                           )

case class ProducerKey(producer_name: String,
                       block_signing_key: String)

case class BSONDate($date: Long)

