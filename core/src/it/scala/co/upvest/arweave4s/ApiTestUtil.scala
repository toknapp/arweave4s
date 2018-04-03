package co.upvest.arweave4s

import co.upvest.arweave4s.adt.{Wallet, Winston, Data}
import co.upvest.arweave4s.utils.CryptoUtils
import cats.implicits._

import scala.util.{Try, Random}
import scala.io.Source

object ApiTestUtil {

  val TestHost = "165.227.40.8:1984"

  object TestAccount {
    lazy val wallet : Wallet = {
      val mkf = Try {
        Source.fromResource("keyfile.json")
      } filter { _.nonEmpty } toOption

      val mev = for {
        s <- sys.env get "TESTNET_ACCOUNT_KEYFILE"
        bs <- CryptoUtils.base64UrlDecode(s)
      } yield Source fromBytes bs


      (mkf orElse mev >>= Wallet.load) get
    }

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

  def randomData(upperBound: Long = 1000000, lowerBound: Long = 0): Data = {
    val bs = new Array[Byte](randomPositiveLong(upperBound, lowerBound).toInt)
    Random.nextBytes(bs)
    new Data(bs)
  }
}
