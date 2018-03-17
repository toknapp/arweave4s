package co.upvest.arweave4s.api

import co.upvest.arweave4s.adt.{Wallet, Address}

import scala.io.Source

object ApiTestUtil {

  val TestHost = "165.227.40.8:1984"

  object TestAccount {
    lazy val wallet  = Wallet.load(Source.fromResource("keyfile.json")).get
    lazy val address = Address.fromEncoded("eIGjur5U0dXas84AO1iuligWMhts8KiLi7phYyGCb0I").get
  }
}
