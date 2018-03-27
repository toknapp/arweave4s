package co.upvest.arweave4s.api.v1

import com.softwaremill.sttp.{HttpURLConnectionBackend, TryHttpURLConnectionBackend}
import co.upvest.arweave4s.adt.Block
import co.upvest.arweave4s.api.{ApiTestUtil, highlevel}
import org.scalatest.{WordSpec, Matchers}

import cats.Id
import cats.instances.try_._

import scala.util.{Try, Success}

class HighlevelSpec extends WordSpec with Matchers {
  import ApiTestUtil._

  import highlevel._

  implicit val idConfig = Config[Id](
    host = TestHost,
    backend = HttpURLConnectionBackend()
  )

  implicit val tryConfig = Config[Try](
    host = TestHost,
    backend = TryHttpURLConnectionBackend()
  )

  "Highlevel" should {
    "block" should {
      import id._
      import monadError._

      val Some(invalidBlockId) = Block.IndepHash.fromEncoded(
        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
      )

      val Some(validBlock) = Block.IndepHash.fromEncoded(
        "cOplqTqwO-JiUUfiJcl-KbD_9qpt3TPVdojgz-QB5h0"
      )

      "return the current block" in {
        block.current[Id]() shouldBe a[Block]
        block.current[Try]() shouldBe a[Success[_]]
      }

      "return a valid block by hash" in {
        block.get[Id](validBlock) shouldBe a[Block]
        block.get[Try](validBlock) shouldBe a[Success[_]]
      }

      "return a valid block by height" in {
        block.get[Id](BigInt(100)) shouldBe a[Block]
        block.get[Try](BigInt(100)) shouldBe a[Success[_]]
      }
    }
  }
}
