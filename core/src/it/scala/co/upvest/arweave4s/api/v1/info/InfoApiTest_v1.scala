package co.upvest.arweave4s.api.v1.info

import co.upvest.arweave4s.adt.Info
import co.upvest.arweave4s.api.v1.marshalling.MarshallerV1
import com.softwaremill.sttp.HttpURLConnectionBackend
import org.scalatest.{Matchers, WordSpec}
import co.upvest.arweave4s.api.v1.info.{info => infoApi}

class InfoApiTest_v1 extends WordSpec with Matchers with MarshallerV1 {

  import co.upvest.arweave4s.api.ApiTestUtil._
  import io.circe.parser._

  "v1 of the info API, on simple backend " when {
    implicit val backend = HttpURLConnectionBackend()
    "asked for Info" should {
      "return a info object" in {
        val response = infoApi.getInfo(TestHost).send()
        // Server should respond OK
        response.statusText shouldBe "OK"
        // Server should respond with Content
        response.body.isRight shouldBe true

        val json = parse(response.body.right.get)
        // Should be a valid JSON
        json.isRight shouldBe true

        json.flatMap(_.as[Info]).isRight shouldBe true
      }
    }
  }
}
