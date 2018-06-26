package co.upvest.arweave4s

import co.upvest.arweave4s.adt.{Wallet, Winston, Data, Transaction}
import co.upvest.arweave4s.utils.CryptoUtils

import scala.util.{Try, Random}
import scala.concurrent.duration._
import scala.io.Source

object ApiTestUtil {

  val NotExistingTestHost = "localhost:2018"
  val TestHost = (sys.env get "TESTNET_HOST" getOrElse "localhost") + ":1984"
  val TestHosts = TestHost + s",$NotExistingTestHost"


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

  def waitForDataTransaction(t: Transaction.Data): Unit = {
    // https://github.com/ArweaveTeam/arweave/blob/d6109b7ad7d824fcea8a540b055c6fb6602b1c81/src/ar_node.erl#L1461
    Thread.sleep(((30 seconds) + (t.data.size * 300 milliseconds) / 1000).toMillis)
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
