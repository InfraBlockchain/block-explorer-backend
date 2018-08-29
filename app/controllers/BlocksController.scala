package controllers

import javax.inject.Inject
import io.swagger.annotations._
import models.BlockJsonFormats._
import models.{Block, BlockRepository}
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by bezalel on 28/08/2018.
  */
@Api(value = "/blocks")
class BlocksController @Inject()(cc: ControllerComponents, blockRepo: BlockRepository) extends AbstractController(cc) {

  @ApiOperation(
    value = "Search all Blocks",
    response = classOf[Block],
    responseContainer = "List"
  )
  def getBlocks = Action.async {
    blockRepo.getBlocks(1, 30).map { blocks =>
      Ok(Json.toJson(blocks))
    }
  }


  @ApiOperation(
    value = "Get a block data by block-id",
    response = classOf[Block]
  )
  @ApiResponses(Array(
    new ApiResponse(code = 404, message = "Block not found")
  )
  )
  def getBlockById(@ApiParam(value = "The id of the Block to fetch") blockId: String) = Action.async {
    blockRepo.getBlockById(blockId).map { maybeBlock =>
      maybeBlock.map { block =>
        Ok(Json.toJson(block))
      }.getOrElse(NotFound)
    }
  }

  @ApiOperation(
    value = "Get a block data by block-number",
    response = classOf[Block]
  )
  @ApiResponses(Array(
    new ApiResponse(code = 404, message = "Block not found")
  )
  )
  def getBlockByBlockNum(@ApiParam(value = "The block number of the Block to fetch") blockNumStr: String) = Action.async {
    blockRepo.getBlockByBlockNum(blockNumStr.toLong).map { maybeBlock =>
      maybeBlock.map { block =>
        Ok(Json.toJson(block))
      }.getOrElse(NotFound)
    }
  }

}
