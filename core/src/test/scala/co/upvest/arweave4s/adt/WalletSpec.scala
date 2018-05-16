package co.upvest.arweave4s.adt

import org.scalatest.{WordSpec, Matchers, Inside}
import io.circe.parser.decode

import scala.util.Success
import scala.io.Source

class WalletSpec extends WordSpec with Matchers with Inside {
  "Wallet" should {
    "be able to read a keyfile" in {
      val s = Source.fromResource("archain_TESTNET_key0d61m0EorUmlTiwzta__V1tLMUjhBLZFIsx4CZzaIqg.json").mkString
      val Some(expected) = Address.fromEncoded("0d61m0EorUmlTiwzta__V1tLMUjhBLZFIsx4CZzaIqg")

      import WalletMarshallers._
      inside(decode[Wallet](s)) {
        case Right(wallet) => wallet.address shouldBe expected
      }
    }

    "should claim to encode in PKCS#8" in {
      Wallet.generate().priv.getFormat shouldBe "PKCS#8"
    }

    "read its on PKCS#8 encoded keys" in {
      val w = Wallet.generate()
      Wallet.fromPKCS8(w.asPKCS8) should matchPattern { case Success(`w`) => }
    }
  }
}
