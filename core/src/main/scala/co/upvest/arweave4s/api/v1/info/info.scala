package co.upvest.arweave4s.api.v1.info
import co.upvest.arweave4s.api.v1
import com.softwaremill.sttp._

object info {

  def getInfo(host: String): Request[String, Nothing] =
    sttp.get(uri"$host/${v1.InfoPath}")

}
