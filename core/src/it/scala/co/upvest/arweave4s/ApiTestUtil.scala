package co.upvest.arweave4s

import co.upvest.arweave4s.adt.{Data, Wallet, Winston}
import co.upvest.arweave4s.utils.CryptoUtils
import com.softwaremill.sttp.{Uri, UriContext}

import scala.util.{Random, Try}
import scala.io.Source

object ApiTestUtil {

  val NotExistingTestHost: Uri = uri"127.0.0.1:9"
  val TestHost: Uri = uri"${(sys.env get "TESTNET_HOST" getOrElse "localhost:1984")}"

  object TestAccount {
    lazy val wallet: Wallet = (
        for {
          s <- Try { Source fromResource "arweave_keyfile_ADECEEQHldVB55AQRg6cq_hhFGKnJiVKN0pRuvp3Sms.json" }.toOption
          w <- Wallet load s
        } yield w
      ) orElse (
        for {
          str <- sys.env get "TESTNET_ACCOUNT_KEYFILE"
          bs <- CryptoUtils base64UrlDecode str
          s = Source fromBytes bs
          w <- Wallet load s
        } yield w
      ) get

    lazy val address = wallet.address
  }

  def randomWinstons(
    upperBound: Winston = Winston.AR,
    lowerBound: Winston = Winston.Zero): Winston = Winston(
      randomPositiveBigInt(
        upperBound = upperBound.amount.toLong, // TODO: horrible .toLong casts
        lowerBound = lowerBound.amount.toLong
      )
    )

  def randomPositiveBigInt(upperBound: Long, lowerBound: Long): BigInt =
    BigInt(randomPositiveLong(upperBound, lowerBound))

  def randomPositiveLong(upperBound: Long, lowerBound: Long): Long =
    (Random.nextLong().abs % (upperBound - lowerBound)) + lowerBound

  def randomBytes(n: Int) = {
    val bs = new Array[Byte](n)
    Random.nextBytes(bs)
    bs
  }

  def randomData(upperBound: Long = 1000000, lowerBound: Long = 0): Data =
    Data(randomBytes(randomPositiveLong(upperBound, lowerBound).toInt))
}
