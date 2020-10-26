package blockchain

import java.util.concurrent.locks.ReentrantReadWriteLock

import akka.actor.ActorSystem
import javax.inject.{Inject, Singleton}
import play.api.{Configuration, Logger}
import play.api.libs.ws._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * Created by bezalel on 13/09/2018.
  */
@Singleton
class InfraBlockchainStatus @Inject()(conf: Configuration, actorSystem: ActorSystem, ws: WSClient) {

  private val INFRABLOCKCHAIN_NODE_CHAIN_API_URL : String = conf.getOptional[String]("infrablockchain.node.chainapi.url").getOrElse("http://localhost:8888")
  private val INFRABLOCKCHAIN_NODE_GET_INFO_API_ENDPOINT = INFRABLOCKCHAIN_NODE_CHAIN_API_URL + "/v1/chain/get_info"

  ///////////////////////////////////////////////////////////
  // Chain Status Data
  private val ReadWriteLockChainStatus = new ReentrantReadWriteLock()
  private val ReadLockChainStatus = ReadWriteLockChainStatus.readLock()
  private val WriteLockChainStatus = ReadWriteLockChainStatus.writeLock()

  private var headBlockNum : Long = 0L
  private var lastIrreversibleBlockNum : Long = 0L

  def getHeadBlockNum() : Long = {
    var headBlock: Long = 0L
    ReadLockChainStatus.lock()
    headBlock = headBlockNum
    ReadLockChainStatus.unlock()
    return headBlock
  }

  def getLastIrreversibleBlockNum() : Long = {
    var lastIrreversibleBlock: Long = 0L
    ReadLockChainStatus.lock()
    lastIrreversibleBlock = lastIrreversibleBlockNum
    ReadLockChainStatus.unlock()
    return lastIrreversibleBlock
  }

  actorSystem.scheduler.schedule(
    initialDelay = 0.microseconds,
    interval = 2.seconds
  ) {

    ws.url(INFRABLOCKCHAIN_NODE_GET_INFO_API_ENDPOINT)
      .withRequestTimeout(5.seconds)
      .get()
      .map { response =>

      /*
      {
        "server_version":"1ce80f62",
        "chain_id":"6376573815dbd2de2d9929027a94aeab3f6e60e87caa953f94ee701ac8425811",
        "head_block_num":372789,
        "last_irreversible_block_num":372726,
        "last_irreversible_block_id":"0005aff6b4b67bbbf6141fef69c7f81ecee4a8dbc2c08b4105e06d0e92d48377",
        "head_block_id":"0005b0355c93d475d56f13c8da13998fdb0f5e2bd082c36b114df5cf931c13e3",
        "head_block_time":"2018-09-13T07:00:07.000",
        "head_block_producer":"producer.c",
        "virtual_block_cpu_limit":200000000,
        "virtual_block_net_limit":1048576000,
        "block_cpu_limit":199900,
        "block_net_limit":1048576,
        "server_version_string":"infrablockchain_v1.2.4_merge-21-g1ce80f6-dirty",
        "native_token_symbol":"4,DKRW"
      }
      */

        val resJs = response.json
        val _chainId = (resJs \ "chain_id").as[String]
        val _headBlockNum = (resJs \ "head_block_num").as[Long]
        val _lastIrreversibleBlockNum = (resJs \ "last_irreversible_block_num").as[Long]

        WriteLockChainStatus.lock()
        headBlockNum = _headBlockNum
        lastIrreversibleBlockNum = _lastIrreversibleBlockNum
        WriteLockChainStatus.unlock()

//        Logger.info("headBlockNum=" + headBlockNum + ", lastIrreversibleBlockNum=" + lastIrreversibleBlockNum)
      }
  }

}
