package co.upvest.arweave4s.api.v1.price

import co.upvest.arweave4s.api.v1.marshalling.MarshallerV1
import com.softwaremill.sttp.HttpURLConnectionBackend
import org.scalatest.{Matchers, WordSpec}

class PriceApiTest_v1 extends WordSpec with Matchers with MarshallerV1 {

  import co.upvest.arweave4s.api.ApiTestUtil._
  import io.circe.parser._

  "v1 of the price API, on simple backend " when {
    implicit val backend = HttpURLConnectionBackend()
    "asked for price " should {
      "return a valid price to given bytesize" in {

        val response = price.getEstimatedTxPrice(TestHost, BigInt(0)).send()
        // Server should respond OK
        response.statusText shouldBe "OK"
        // Server should respond with Content
        response.body.isRight shouldBe true

        val json = parse(response.body.right.get)
        // Should be a valid JSON
        json.isRight shouldBe true
        // Should be a valid JSON list
        json.flatMap(_.as[BigInt]).isRight shouldBe true

        json.flatMap(_.as[BigInt]).foreach(_ shouldBe BigInt(0))
      }

      "return a valid price proportionaly to bytesize" in {
        val response0 = price.getEstimatedTxPrice(TestHost, BigInt(1)).send()
        val response1 = price.getEstimatedTxPrice(TestHost, BigInt(2)).send()
        val json0     = parse(response0.body.right.get)
        val json1     = parse(response1.body.right.get)

        val price0 = json0.flatMap(_.as[BigInt]).getOrElse(BigInt(0))
        val price1 = json1.flatMap(_.as[BigInt]).getOrElse(BigInt(0))

        (price0 > 0) shouldBe true
        (price1 > 0) shouldBe true

        price1 / 2 shouldBe price0

      }
    }
  }

}
