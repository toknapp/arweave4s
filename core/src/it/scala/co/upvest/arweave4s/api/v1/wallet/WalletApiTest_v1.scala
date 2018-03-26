package co.upvest.arweave4s.api.v1.wallet

import co.upvest.arweave4s.adt._
import co.upvest.arweave4s.api.v1.tx.tx
import co.upvest.arweave4s.api.v1.marshalling.MarshallerV1
import com.softwaremill.sttp.HttpURLConnectionBackend
import org.scalatest.{Matchers, WordSpec, Inside}

class WalletApiTest_v1 extends WordSpec with Matchers with MarshallerV1 with Inside {

  import co.upvest.arweave4s.api.ApiTestUtil._
  import io.circe.parser._

  "v1 of the wallet API, on simple backend " when {
    implicit val backend = HttpURLConnectionBackend()
    val Some(validAddress) =
      Address.fromEncoded("0MMYwTxRbXpK0PZvm3XgDABpmfGRUEfah0nF6QcfcPg")

    "asked for a wallet" should {
      "return a valid wallet balance" in {
        val response = wallet
          .getBalanceViaAddress(
            TestHost,
            validAddress.toString
          )
          .send()

        response.code shouldBe 200
        inside(response.body) {
          case Right(body) =>
            parse(body) flatMap { _.as[Winston] } should matchPattern {
              case Right(_) =>
            }
        }
      }

      "return a valid transaction via address" in {
        val r1 = wallet
          .getLastTxViaAddress(
            TestHost,
            TestAccount.address.toString
          )
          .send()

        r1.code shouldBe 200

        inside(r1.body) { case Right(b1) =>
          inside(Transaction.Id.fromEncoded(b1)) { case Some(txId) =>
            val r2 = tx.getTxViaId(TestHost, txId.toString).send()
            r2.code shouldBe 200
            inside(r2.body) { case Right(b2) =>
              inside(parse(b2) flatMap { _.as[Signed[Transaction]] }) {
                case Right(Signed(tx, _)) =>
                  tx.id shouldBe txId
              }
            }
          }
        }
      }
    }
  }
}
