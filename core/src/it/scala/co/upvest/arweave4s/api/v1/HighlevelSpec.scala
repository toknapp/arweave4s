package co.upvest.arweave4s.api.v1

import com.softwaremill.sttp.{HttpURLConnectionBackend, TryHttpURLConnectionBackend}
import co.upvest.arweave4s.adt.{Block, Transaction, Wallet, Winston}
import co.upvest.arweave4s.api.{ApiTestUtil, highlevel}
import org.scalatest.{WordSpec, Matchers, Inside}

import cats.{Id, ~>}
import cats.arrow.FunctionK
import cats.instances.try_._

import scala.util.Try

class HighlevelSpec extends WordSpec with Matchers with Inside {
  import ApiTestUtil._
  import highlevel._

  val idConfig = Config(host = TestHost, HttpURLConnectionBackend())
  val tryConfig = Config(host = TestHost, TryHttpURLConnectionBackend())

  val Some(invalidBlockHash) = Block.IndepHash.fromEncoded("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
  val invalidBlockHeight = BigInt(Long.MaxValue)
  val Some(validBlock) = Block.IndepHash.fromEncoded("cOplqTqwO-JiUUfiJcl-KbD_9qpt3TPVdojgz-QB5h0")
  val Some(validTxId) = Transaction.Id.fromEncoded("3MFrfH0-HI9GeMfFAwIhK9TcASsxDJeK6MFMbJplSkU")

  implicit val idRunner: Id ~> Id = FunctionK.id
  implicit val tryRunner: Try ~> Id = λ[Try ~> Id]{ _.get }

  def highlevelApi[F[_]](c: Config[F])(implicit
    jh: JsonHandler[F],
    esh: EncodedStringHandler[F],
    sh: SuccessHandler[F],
    run: F ~> Id) {
      implicit val _ = c

      "the block api" should {
        "return the current block" in {
          run { block.current() } shouldBe a[Block]
        }

        "return a valid block by hash" in {
          run { block.get(validBlock) } shouldBe a[Block]
        }

        "return a valid block by height" in {
          run { block.get(BigInt(100)) } shouldBe a[Block]
        }

        "fail when a block does not exist (by hash)" in {
          assertThrows[HttpFailure] { run { block.get(invalidBlockHash) } }
        }

        "fail when a block does not exist (by height)" in {
          assertThrows[HttpFailure] { run { block.get(invalidBlockHeight) } }
        }
      }

      "the wallet api" should {
        val arbitraryWallet = Wallet.generate()

        "return last transaction id" in {
          inside(run { address.lastTx(TestAccount.address) }) {
            case Some(txId) => txId shouldBe a[Transaction.Id]
          }
        }

        "return none when no last transaction" in {
          run { address.lastTx(arbitraryWallet.address) } shouldBe empty
        }

        "return a positive balance" in {
          run { address.balance(TestAccount.address) }
            .amount should be > BigInt(0)
        }

        "return a zero balance" in {
          run { address.balance(arbitraryWallet) } shouldBe Winston.Zero
        }
      }

      "the transaction api" should {
        "return valid transaction" in {
          run { tx.get(validTxId) }.id shouldBe validTxId
        }

        "submit transaction" in {
          val owner = Wallet.generate()

          val stx = Transaction.Transfer(
            Transaction.Id.generate(),
            run { address.lastTx(owner) },
            owner,
            Wallet.generate().address,
            quantity = randomWinstons(),
            reward = randomWinstons()
          ).sign(owner)

          run { tx.submit(stx) } shouldBe (())
        }
      }

      "tre price api" should {
        "return a valid (positive) price" in {
          run { price.estimateForBytes(BigInt(10)) }
            .amount should be > BigInt(0)
        }

        "return a price proportionate in amount of bytes" in {
          val x = randomPositiveBigInt(10000)
          val q = randomPositiveBigInt(100)
          val y = x * q

          val px = run { price.estimateForBytes(x) }.amount
          val py = run { price.estimateForBytes(y) }.amount

          py / px shouldBe q
        }
      }
    }

  "Highlevel" should {
    "using Id" should {
      import id._
      highlevelApi(idConfig)
    }

    "using Try" should {
      import monadError._
      highlevelApi(tryConfig)
    }
  }
}
