package co.upvest.arweave4s.api.v1.block

import co.upvest.arweave4s.api.v1.marshalling.MarshallerV1
import com.softwaremill.sttp.HttpURLConnectionBackend
import org.scalatest.{Matchers, WordSpec, Inside}

class BlockApiTest_v1 extends WordSpec with Matchers with MarshallerV1 with Inside {

  import io.circe.parser._
  import co.upvest.arweave4s.adt._
  import co.upvest.arweave4s.api.ApiTestUtil._

  "v1 of the block API, on simple backend " when {
    implicit val backend = HttpURLConnectionBackend()
    val Some(validBlock) = Block.IndepHash.fromEncoded(
      "cOplqTqwO-JiUUfiJcl-KbD_9qpt3TPVdojgz-QB5h0"
    )
    val validBlockheight = BigInt(100)

    "asked for block to BlockId" should {
      "return a valid block" in {

        val response = block.getBlockViaId(TestHost, validBlock).send()
        response.code shouldBe 200

        inside(response.body) {
          case Right(body) =>
            inside(parse(body) flatMap { _.as[Block] }) {
              case Right(b) => b.indepHash shouldBe validBlock
            }
        }
      }

      "return an valid response if block hash is empty" in {
        pending

        val invalidBlockId = new Block.IndepHash(Array.empty)
        val response       = block.getBlockViaId(TestHost, invalidBlockId).send()
        response.code shouldBe 404
      }

      "return an valid response if block does not exist" in {
        val Some(invalidBlockId) = Block.IndepHash.fromEncoded(
          "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        )
        val response = block.getBlockViaId(TestHost, invalidBlockId).send()
        response.code shouldBe 404
      }

      "return an valid response by block height" in {
        val response = block
          .getBlockViaHeight(
            TestHost,
            validBlockheight
          )
          .send()

        response.code shouldBe 200

        inside(response.body) {
          case Right(body) =>
            inside(parse(body) flatMap { _.as[Block] }) {
              case Right(b) => b.height shouldBe validBlockheight
            }
        }
      }

      "return an valid response if block does not exist by height" in {
        block
          .getBlockViaHeight(
            TestHost,
            BigInt("101010101010101010")
          )
          .send()
          .code shouldBe 404
      }
    }
  }

}
