package co.upvest.arweave4s.api.v1.wallet

import co.upvest.arweave4s.adt._
import co.upvest.arweave4s.api.v1.marshalling.MarshallerV1
import com.softwaremill.sttp.HttpURLConnectionBackend
import org.scalatest.{Matchers, WordSpec}

class WalletApiTest_v1 extends WordSpec with Matchers with MarshallerV1 {

  import co.upvest.arweave4s.api.ApiTestUtil._
  import io.circe.parser._

  "v1 of the wallet API, on simple backend " when {
    implicit val backend = HttpURLConnectionBackend()
    val validAddress     = Address("0MMYwTxRbXpK0PZvm3XgDABpmfGRUEfah0nF6QcfcPg")

    "asked for a wallet" should {
      "return a valid wallet balance" in {

        val response = wallet.getBalanceViaAddress(TestHost, validAddress.toString()).send()
        // Server should respond OK
        response.statusText shouldBe "OK"
        // Server should respond with Content
        response.body.isRight shouldBe true

        val json = parse(response.body.right.get)

        // Should be a valid JSON
        json.isRight shouldBe true

        // Should be a valid number
        json.flatMap(_.as[BigInt]).isRight shouldBe true
      }

      "return a valid transaction via address" in {
        val response = wallet.getLastTxViaAddress(TestHost, Address("lDYPXHUth-DmJkoaj1hoyyjnY0YAzrgR2lbqSbg6G6Q").toString()).send()
        // Server should respond OK
        response.statusText shouldBe "OK"
        // Server should respond with Content
        response.body.isRight shouldBe true

        Id.fromB64urlEncoded(response.body.right.get) shouldBe Id.fromB64urlEncoded("pYBnnXu6Bdd5EcsyrjNfdE07s8fj_AG7qsEiZW58H_o")
      }
    }
  }
}
