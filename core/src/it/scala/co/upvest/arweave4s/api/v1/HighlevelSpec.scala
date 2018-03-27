package co.upvest.arweave4s.api.v1

import com.softwaremill.sttp.{HttpURLConnectionBackend, TryHttpURLConnectionBackend}
import co.upvest.arweave4s.adt.{Block, Transaction, Wallet, Winston}
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

    "address" should {
      val arbitraryWallet = Wallet.generate()

      "using Id functor" should {
        import id._
        implicit val _ = idConfig

        "return last transaction id" in {
          inside(address.lastTx(TestAccount.address)) {
            case Some(txId) => txId shouldBe a[Transaction.Id]
          }
        }

        "return none when no last transaction" in {
          address.lastTx(arbitraryWallet.address) shouldBe empty
        }

        "return a positive balance" in {
          address.balance(TestAccount.address).amount should be > BigInt(0)
        }

        "return a zero balance" in {
          address.balance(arbitraryWallet) shouldBe Winston.Zero
        }
      }

      "using Try functor" should {
        import monadError._
        implicit val _ = tryConfig

        "return last transaction id" in {
          inside(address.lastTx(TestAccount.address)) {
            case Success(Some(txId)) => txId shouldBe a[Transaction.Id]
          }
        }

        "return none when no last transaction" in {
          address.lastTx(arbitraryWallet.address) should matchPattern {
            case Success(None) =>
          }
        }

        "return a positive balance" in {
          inside(address.balance(TestAccount.address)) {
            case Success(Winston(amount)) => amount should be > BigInt(0)
          }
        }

        "return a zero balance" in {
          address.balance(arbitraryWallet) should matchPattern {
            case Success(Winston.Zero) =>
          }
        }
      }
    }

    "tx" should {
      val Some(validTxId) = Transaction.Id.fromEncoded(
        "3MFrfH0-HI9GeMfFAwIhK9TcASsxDJeK6MFMbJplSkU"
      )

      "using Id functor" should {
        import id._
        implicit val _ = idConfig

        "return valid transaction" in {
          tx.get(validTxId).id shouldBe validTxId
        }
      }

      "using Try functor" should {
        import monadError._
        implicit val _ = tryConfig

        "return valid transaction" in {
          inside(tx.get(validTxId)) {
            case Success(tx) => tx.id shouldBe validTxId
          }
        }
      }
    }
  }
}
