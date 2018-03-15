package co.upvest.arweave4s.api.v1.wallet

import co.upvest.arweave4s.adt._
import co.upvest.arweave4s.api.v1.marshalling.MarshallerV1
import com.softwaremill.sttp.HttpURLConnectionBackend
import org.scalatest.{Matchers, WordSpec, Inside}

class WalletApiTest_v1 extends WordSpec
  with Matchers with MarshallerV1 with Inside {

  import co.upvest.arweave4s.api.ApiTestUtil._
  import io.circe.parser._

  "v1 of the wallet API, on simple backend " when {
    implicit val backend = HttpURLConnectionBackend()
    val Some(validAddress) =
      Address.fromEncoded("0MMYwTxRbXpK0PZvm3XgDABpmfGRUEfah0nF6QcfcPg")

    "asked for a wallet" should {
      "return a valid wallet balance" in {
        val response = wallet.getBalanceViaAddress(
          TestHost,
          validAddress
        ).send()

        response.code shouldBe 200
        inside (response.body) { case Right(body) =>
          parse(body) flatMap { _.as[BigInt] } should matchPattern {
            case Right(_) =>
          }
        }
      }

      "return a valid transaction via address" in {
        val response = wallet.getLastTxViaAddress(
          TestHost,
          TestAccount.address
        ).send()

        response.code shouldBe 200

        inside(response.body) { case Right(body) =>
          val Some(actualTx) = Transaction.Id.fromEncoded(body)
          val Some(expectedTx) = Transaction.Id.fromEncoded(
            "3MFrfH0-HI9GeMfFAwIhK9TcASsxDJeK6MFMbJplSkU"
          )
          actualTx shouldBe expectedTx
        }
      }
    }
  }
}
