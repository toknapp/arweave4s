package co.upvest.arweave4s.api.v1.block

import co.upvest.arweave4s.api.v1.marshalling.MarshallerV1
import com.softwaremill.sttp.HttpURLConnectionBackend
import org.scalatest.{Matchers, WordSpec}

class BlockApiTest_v1 extends WordSpec with Matchers with MarshallerV1 {

  import io.circe.parser._
  import co.upvest.arweave4s.adt._
  import co.upvest.arweave4s.api.ApiTestUtil._

  "v1 of the block API, on simple backend " when {
    implicit val backend = HttpURLConnectionBackend()
    val validBlock       = Block.Id.fromEncoded("l5rg-UfwB65SAr-E5vy5rkXIIHW8uqEq64KnTJiVvLg")
    val validBlockheight = BigInt(634)
    "asked for block to BlockId" should {
      "return a valid block" in {

        val response = block.getBlockViaId(TestHost, validBlock.toString).send()
        // Server should respond OK
        response.statusText shouldBe "OK"
        // Server should respond with Content
        response.body.isRight shouldBe true

        val json = parse(response.body.right.get)
        // Should be a valid JSON
        json.isRight shouldBe true
        // Should be a valid JSON list
        // should be a valid list of peers
        json.flatMap(_.as[Block]).isRight shouldBe true
      }

      "return an valid response if block does not exist" in {

        val response = block.getBlockViaId(TestHost, "invalidblock").send()
        // Server should respond OK
        assert(response.statusText == "Not Found")
      }

      "return an valid response by block height" in {

        val response = block.getBlockViaHeight(TestHost, validBlockheight).send()
        // Server should respond OK
        response.statusText shouldBe "OK"

        // Server should respond with Content
        response.body.isRight shouldBe true

        val json = parse(response.body.right.get)
        // Should be a valid JSON
        json.isRight shouldBe true
        // Should be a valid JSON list
        // should be a valid list of peers
        json.flatMap(_.as[Block]).isRight shouldBe true
      }

      "return an valid response if block does not exist by height" in {

        val response = block.getBlockViaHeight(TestHost, BigInt("101010101010101010")).send()
        // Server should respond OK
        assert(response.statusText == "Not Found")
      }
    }
  }

}
