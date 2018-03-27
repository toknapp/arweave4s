package co.upvest.arweave4s.api.v1

import com.softwaremill.sttp.{HttpURLConnectionBackend, TryHttpURLConnectionBackend}
import co.upvest.arweave4s.adt.Block
import co.upvest.arweave4s.api.{ApiTestUtil, highlevel}
import org.scalatest.{WordSpec, Matchers, Inside}

import cats.Id
import cats.instances.try_._

import scala.util.{Try, Success, Failure}

class HighlevelSpec extends WordSpec with Matchers with Inside {
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
      val Some(invalidBlockHash) = Block.IndepHash.fromEncoded(
        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
      )

      val invalidBlockHeight = BigInt(Long.MaxValue)

      val Some(validBlock) = Block.IndepHash.fromEncoded(
        "cOplqTqwO-JiUUfiJcl-KbD_9qpt3TPVdojgz-QB5h0"
      )

      "using Id functor" should {
        import id._

        "return the current block" in {
          block.current[Id]() shouldBe a[Block]
        }

        "return a valid block by hash" in {
          block.get[Id](validBlock) shouldBe a[Block]
        }

        "return a valid block by height" in {
          block.get[Id](BigInt(100)) shouldBe a[Block]
        }

        "fail when a block does not exist (by hash)" in {
          assertThrows[HttpFailure] { block.get[Id](invalidBlockHash) }
        }

        "fail when a block does not exist (by height)" in {
          assertThrows[HttpFailure] { block.get[Id](invalidBlockHeight) }
        }
      }

      "using Try" should {
        import monadError._

        "return the current block" in {
          block.current[Try]() shouldBe a[Success[_]]
        }

        "return a valid block by hash" in {
          block.get[Try](validBlock) shouldBe a[Success[_]]
        }

        "return a valid block by height" in {
          block.get[Try](BigInt(100)) shouldBe a[Success[_]]
        }

        "fail when a block does not exist (by hash)" in {
          inside(block.get[Try](invalidBlockHash)) {
            case Failure(HttpFailure(rsp)) => rsp.code shouldBe 404
          }
        }

        "fail when a block does not exist (by height)" in {
          inside(block.get[Try](invalidBlockHeight)) {
            case Failure(HttpFailure(rsp)) => rsp.code shouldBe 404
          }
        }
      }
    }
  }
}
