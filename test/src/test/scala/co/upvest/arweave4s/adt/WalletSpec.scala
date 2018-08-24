package co.upvest.arweave4s.adt

import org.scalatest.{WordSpec, Matchers, Inside}
import org.scalatest.prop.GeneratorDrivenPropertyChecks

import io.circe.parser.decode
import io.circe.syntax._

import scala.util.Success
import scala.io.Source

class WalletSpec extends WordSpec
  with Matchers with Inside with GeneratorDrivenPropertyChecks
  with ArbitraryInstances {
  "Wallet" should {
    "be able to read a keyfile" in {
      val s = Source.fromResource("arweave_keyfile_J1ahU758MJXKLUgvbIt-iFRZjBzFc2caSIcUmIJwIdg.json").mkString
      val Some(expected) = Address.fromEncoded("J1ahU758MJXKLUgvbIt-iFRZjBzFc2caSIcUmIJwIdg")

      import WalletMarshallers._
      inside(decode[Wallet](s)) {
        case Right(wallet) => wallet.address shouldBe expected
      }
    }

    "be able to read its own json codec" in {
      forAll (minSuccessful(10)) { w: Wallet =>
        decode[Wallet](w.asJson.noSpaces) should matchPattern {
          case Right(`w`) =>
        }
      }
    }

    "claim to encode in PKCS#8" in {
      Wallet.generate().priv.getFormat shouldBe "PKCS#8"
    }

    "read its on PKCS#8 encoded keys" in {
      forAll (minSuccessful(10)) { w: Wallet =>
        Wallet.fromPKCS8(w.asPKCS8) should matchPattern { case Success(`w`) => }
      }
    }
  }
}
