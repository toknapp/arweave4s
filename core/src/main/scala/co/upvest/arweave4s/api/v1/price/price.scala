package co.upvest.arweave4s.api.v1.price

import co.upvest.arweave4s.api.v1
import com.softwaremill.sttp._

object price {

  def getEstimatedTxPrice(host: String, bytes: BigInt): Request[String, Nothing] =
    sttp.get(uri"$host/${v1.PricePath}/$bytes")
}
