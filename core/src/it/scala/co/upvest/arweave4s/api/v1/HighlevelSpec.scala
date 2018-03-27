package co.upvest.arweave4s.api.v1

import com.softwaremill.sttp.{HttpURLConnectionBackend, TryHttpURLConnectionBackend}
import co.upvest.arweave4s.adt.Block
import co.upvest.arweave4s.api.{ApiTestUtil, highlevel}
import org.scalatest.{WordSpec, Matchers, Inside}

import cats.instances.try_._

import scala.util.{Success, Failure}

class HighlevelSpec extends WordSpec with Matchers with Inside {
  import ApiTestUtil._
  import highlevel._

  val idConfig = Config(
    host = TestHost,
    backend = HttpURLConnectionBackend()
  )

  val tryConfig = Config(
    host = TestHost,
    backend = TryHttpURLConnectionBackend()
  )

  "Highlevel" should {
    "block" should {
      val Some(invalidBlockHash) = Block.IndepHash.fromEncoded(
        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
      )

      val invalidBlockHeight = BigInt(Long.MaxValue)

      val Some(validBlock) = Block.IndepHash.fromEncoded(
        "cOplqTqwO-JiUUfiJcl-KbD_9qpt3TPVdojgz-QB5h0"
      )

      "using Id functor" should {
        import id._
        implicit val _ = idConfig

        "return the current block" in {
          block.current() shouldBe a[Block]
        }

        "return a valid block by hash" in {
          block.get(validBlock) shouldBe a[Block]
        }

        "return a valid block by height" in {
          block.get(BigInt(100)) shouldBe a[Block]
        }

        "fail when a block does not exist (by hash)" in {
          assertThrows[HttpFailure] { block.get(invalidBlockHash) }
        }

        "fail when a block does not exist (by height)" in {
          assertThrows[HttpFailure] { block.get(invalidBlockHeight) }
        }
      }

      "using Try" should {
        import monadError._
        implicit val _ = tryConfig

        "return the current block" in {
          block.current() shouldBe a[Success[_]]
        }

        "return a valid block by hash" in {
          block.get(validBlock) shouldBe a[Success[_]]
        }

        "return a valid block by height" in {
          block.get(BigInt(100)) shouldBe a[Success[_]]
        }

        "fail when a block does not exist (by hash)" in {
          inside(block.get(invalidBlockHash)) {
            case Failure(HttpFailure(rsp)) => rsp.code shouldBe 404
          }
        }

        "fail when a block does not exist (by height)" in {
          inside(block.get(invalidBlockHeight)) {
            case Failure(HttpFailure(rsp)) => rsp.code shouldBe 404
          }
        }
      }
    }
  }
}
