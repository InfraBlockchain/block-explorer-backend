package controllers

import javax.inject.Inject
import io.swagger.annotations._
import repositories.BlockJsonFormats._
import repositories.{Block, BlockRepository}
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
  def getBlocks(@ApiParam(value = "page number to fetch recent block list, first page = 1 (default : 1)") page: Int,
                @ApiParam(value = "the number of blocks in current page (default : 30)") size: Int) = Action.async {
    blockRepo.getBlocks(page, size).map { blocks =>
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
      maybeBlock.map { blockRawJs =>
        Ok(blockRawJs)
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
  def getBlockByBlockNum(@ApiParam(value = "The block number of the Block to fetch") blockNum: Long) = Action.async {
    blockRepo.getBlockByBlockNum(blockNum).map { maybeBlock =>
      maybeBlock.map { blockRawJs =>
        Ok(blockRawJs)
      }.getOrElse(NotFound)
    }
  }

}
