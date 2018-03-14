package co.upvest.arweave4s.api.v1.block

import co.upvest.arweave4s.api.v1
import co.upvest.arweave4s.adt.Block
import com.softwaremill.sttp._

object block {

  def getBlockViaId(
    host: String,
    blockId: Block.IndepHash
  ): Request[String, Nothing] =
    sttp.get(uri"$host/${v1.BlockPath}/hash/$blockId")

  def getBlockViaHeight(
    host: String,
    height: BigInt): Request[String, Nothing] =
      sttp.get(uri"$host/${v1.BlockPath}/height/$height")
}
