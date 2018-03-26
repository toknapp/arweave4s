package co.upvest.arweave4s.api

import co.upvest.arweave4s.adt.Wallet
import co.upvest.arweave4s.utils.CryptoUtils
import cats.implicits._

import scala.util.Try
import scala.io.Source

object ApiTestUtil {

  val TestHost = "165.227.40.8:1984"

  object TestAccount {
    lazy val wallet = {
      val mkf = Try {
        Source.fromResource("keyfile.json")
      } filter { _.nonEmpty } toOption

      val mev = for {
        s <- sys.env get "TESTNET_ACCOUNT_KEYFILE"
        bs <- CryptoUtils.base64UrlDecode(s)
      } yield Source fromBytes bs

      mkf orElse mev >>= Wallet.load get
    }

    lazy val address = wallet.address
  }
}
