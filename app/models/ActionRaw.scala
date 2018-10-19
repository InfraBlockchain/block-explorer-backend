package models

import play.api.libs.json.{JsObject, JsValue}

/**
  * Created by bezalel on 10/09/2018.
  */
case class ActionRaw(receipt: ActionReceipt,
                     act: ActionItem,
                     context_free: Boolean,
                     elapsed: Long,
                     cpu_usage: Long,
                     console: String,
                     total_cpu_usage: Long,
                     trx_id: String,
                     block_num: Int,
                     block_time: String,
                     producer_block_id: Option[String],
                     account_ram_deltas: Option[Seq[AccountRamDelta]],
                     sender: Option[Seq[String]],
                     parent: Option[Long]
                    )

case class ActionRawList(lastIrrBlkNum: Long,
                         actions: Seq[ActionRaw])

case class ActionReceipt(receiver: String,
                         act_digest: String,
                         global_sequence: Long,
                         recv_sequence: Long,
                         auth_sequence: Seq[Seq[JsValue]],
                         code_sequence: Int,
                         abi_sequence: Int
                        )

case class ActionItem(account: String,
                      name: String,
                      authorization: Seq[PermissionLevel],
                      data: JsObject,
                      hex_data: String
                     )

case class PermissionLevel(actor: String,
                           permission: String)

case class ActionTrace(receipt: ActionReceipt,
                       act: ActionItem,
                       elapsed: Long,
                       cpu_usage: Long,
                       console: String,
                       total_cpu_usage: Long,
                       trx_id: String,
                       inline_traces: ActionTrace)

case class AccountRamDelta(account: String,
                           delta: Long)