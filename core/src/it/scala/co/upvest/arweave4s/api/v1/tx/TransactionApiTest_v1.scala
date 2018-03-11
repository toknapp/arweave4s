package co.upvest.arweave4s.api.v1.tx

import co.upvest.arweave4s.adt.Transaction.Signed
import co.upvest.arweave4s.api.v1.marshalling.MarshallerV1
import com.softwaremill.sttp.HttpURLConnectionBackend
import org.scalatest.{Matchers, WordSpec}

class TransactionApiTest_v1 extends WordSpec with Matchers with MarshallerV1 {

  import co.upvest.arweave4s.adt._
  import co.upvest.arweave4s.api.ApiTestUtil._
  import io.circe.parser._

  "v1 of the transaction API, on simple backend " when {

    implicit val backend = HttpURLConnectionBackend()

    val transactionId = Id.fromB64urlEncoded("FDqgPohAc15sR0nZjtSo45fa1bzA6kcrigVtw4vSGIM")
    "asked for a full Tx to TxId" should {
      "return a valid Transaction" in {

        val response = tx.getTxViaId(TestHost, transactionId.toString).send()
        // Server should respond OK
        response.statusText shouldBe "OK"
        // Server should respond with Content
        response.body.isRight shouldBe true

        val json = parse(response.body.right.get)
        // Should be a valid JSON
        json.isRight shouldBe true
        // Should be a valid JSON list
        // should be a valid list of peers
        json.flatMap(_.as[Signed]).isRight shouldBe true
      }

      "return tx fields by filter" in {
        val response = tx.getTxViaId(TestHost, transactionId.toString).send()
        val json     = parse(response.body.right.get)
        val transaction = json
          .flatMap(_.as[Signed])
          .getOrElse(throw new IllegalStateException("Could not fetch tx"))

        val filteredResponse = tx.getFilteredTxViaId(TestHost, transactionId.toString, "id").send()
        val id               = Id.fromB64urlEncoded(filteredResponse.body.right.get)

        id shouldBe transaction.id
      }

      "return tx data body as HTML" in {
        val response = tx.getBodyAsHtml(TestHost, transactionId.toString).send()
        response.statusText shouldBe "OK"
        // Should be tests with an transaction with non empty data.
      }

      "submitting an valid transaction" in {}
    }
  }

}
