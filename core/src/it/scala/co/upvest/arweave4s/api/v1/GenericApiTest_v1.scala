package co.upvest.arweave4s.api.v1

import co.upvest.arweave4s.api.v1.marshalling.MarshallerV1
import com.softwaremill.sttp.HttpURLConnectionBackend
import org.scalatest.{Matchers, WordSpec}

class GenericApiTest_v1 extends WordSpec with Matchers with MarshallerV1 {

  import io.circe.parser._
  import co.upvest.arweave4s.adt._
  import co.upvest.arweave4s.api.ApiTestUtil._

  "v1 of the API, on simple backend " when {
    implicit val backend = HttpURLConnectionBackend()
    "asked for data" should {
      "return an peer list" in {
        val response = v1.getPeersList(TestHost).send()
        // Server should respond OK
        response.statusText shouldBe "OK"
        // Server should respond with Content
        response.body.isRight shouldBe true

        val json = parse(response.body.right.get)
        // Should be a valid JSON
        json.isRight shouldBe true
        // Should be a valid JSON list
        json.map(_.hcursor.downArray).isRight shouldBe true
        // should be a valid list of peers
        json.flatMap(_.as[Seq[Peer]]).isRight shouldBe true
      }

      "return current block " in {
        val response = v1.getCurrentBlock("http://178.62.4.18:1984").send()
        response.statusText shouldBe "OK"
        response.body.isRight shouldBe true
        val json = parse(response.body.right.get)

        // Should be a valid JSON
        json.isRight shouldBe true

        // Should be a valid Block
        json.flatMap(_.as[Block]).isRight shouldBe true
      }
    }
  }
}
