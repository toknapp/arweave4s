package co.upvest.arweave4s.api.v1

import com.softwaremill.sttp.HttpURLConnectionBackend
import co.upvest.arweave4s.adt.Block
import co.upvest.arweave4s.api.{ApiTestUtil, highlevel}
import org.scalatest.{WordSpec, Matchers}

import cats.Id

class HighlevelSpec extends WordSpec with Matchers {
  import ApiTestUtil._

  import highlevel._

  implicit val config = Config[Id, Nothing](
    host = TestHost,
    backend = HttpURLConnectionBackend()
  )

  "Highlevel" should {
    "block" should {

      import id._
      "return the current block" in {
        block.current[Id, Nothing]() shouldBe a[Block]
      }

      "return a valid block by hash" in {
        val Some(validBlock) = Block.IndepHash.fromEncoded(
          "cOplqTqwO-JiUUfiJcl-KbD_9qpt3TPVdojgz-QB5h0"
        )
        block.get[Id, Nothing](validBlock) shouldBe a[Block]
      }

      "return a valid block by height" in {
        block.get[Id, Nothing](BigInt(100)) shouldBe a[Block]
      }
    }
  }
}
