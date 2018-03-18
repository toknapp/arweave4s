package co.upvest.arweave4s.api.v1

import com.softwaremill.sttp._

object v1 {

  def getCurrentBlock(host: String): Request[String, Nothing] =
    sttp.get(uri"$host/current_block")

  def getPeersList(host: String): Request[String, Nothing] =
    sttp.get(uri"$host/peers")
}
