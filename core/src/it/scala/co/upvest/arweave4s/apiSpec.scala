package co.upvest.arweave4s

import com.softwaremill.sttp.{HttpURLConnectionBackend, TryHttpURLConnectionBackend}
import com.softwaremill.sttp.asynchttpclient.future.AsyncHttpClientFutureBackend
import co.upvest.arweave4s.adt.{Block, Transaction, Wallet, Winston}
import org.scalatest.{WordSpec, Matchers, Inside}
import org.scalatest.concurrent.{ScalaFutures, IntegrationPatience}

import cats.{Id, ~>}
import cats.data.EitherT
import cats.arrow.FunctionK
import cats.instances.try_._
import cats.instances.future._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import scala.util.Try

class apiSpec extends WordSpec
  with Matchers with Inside with ScalaFutures
  with IntegrationPatience {
  import ApiTestUtil._
  import api._

  val idConfig = Config(host = TestHost, HttpURLConnectionBackend())
  val tryConfig = Config(host = TestHost, TryHttpURLConnectionBackend())

  implicit val ec = ExecutionContext.global

  val futureConfig = FullConfig[EitherT[Future, Failure, ?], Future](
      host = TestHost,
      AsyncHttpClientFutureBackend(),
      i = λ[Future ~> EitherT[Future, Failure, ?]]{ EitherT liftF _ }
    )

  val Some(invalidBlockHash) = Block.IndepHash.fromEncoded("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
  val invalidBlockHeight = BigInt(Long.MaxValue)
  val Some(validBlock) = Block.IndepHash.fromEncoded("cOplqTqwO-JiUUfiJcl-KbD_9qpt3TPVdojgz-QB5h0")
  val Some(validTxId) = Transaction.Id.fromEncoded("3MFrfH0-HI9GeMfFAwIhK9TcASsxDJeK6MFMbJplSkU")

  implicit val idRunner: Id ~> Id = FunctionK.id
  implicit val tryRunner: Try ~> Id = λ[Try ~> Id]{ _.get }
  implicit val eTFRunner: λ[α => EitherT[Future, Failure, α]] ~> Id =
    λ[λ[α => EitherT[Future, Failure, α]] ~> Id] { eTF =>
      whenReady(eTF.value) {
        case Left(e) => throw e
        case Right(a) => a
      }
    }

  def apiBehavior[F[_], G[_]](c: AbstractConfig[F, G])(implicit
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
          run { address.lastTx(arbitraryWallet) } shouldBe empty
        }

        "return a positive balance" in {
          run { address.balance(TestAccount.address) }
            .amount should be > BigInt(0)
        }

        "return a zero balance" in {
          run { address.balance(arbitraryWallet) } shouldBe Winston.Zero
        }
      }

      "tre price api" should {
        "return a valid (positive) price" in {
          run { price.estimateForBytes(BigInt(10)) }
            .amount should be > BigInt(0)
        }

        "return a price proportionate in amount of bytes" in {
          val x = randomPositiveBigInt(10000, 0)
          val q = randomPositiveBigInt(100, 0)
          val y = x * q

          val px = run { price.estimateForBytes(x) }.amount
          val py = run { price.estimateForBytes(y) }.amount

          py / px shouldBe q
        }
      }

      "the transaction api" should {
        "return valid transaction" in {
          run { tx.get(validTxId) }.id shouldBe validTxId
        }

        "submit a transfer transaction" in {
          val owner = Wallet.generate()

          val stx = Transaction.Transfer(
            Transaction.Id.generate(),
            run { address.lastTx(owner) },
            owner,
            Wallet.generate(),
            quantity = randomWinstons(),
            reward = randomWinstons()
          ).sign(owner)

          run { tx.submit(stx) } shouldBe (())
        }

        "submit a data transaction" in {
          val owner = Wallet.generate()
          val data = randomData()
          val estCost = run { price.estimate(data) }

          val stx = Transaction.Data(
            Transaction.Id.generate(),
            run { address.lastTx(owner) },
            owner,
            data,
            reward = estCost
          ).sign(owner)

          run { tx.submit(stx) } shouldBe (())
        }
      }
    }

  "api" should {
    "using Id" should {
      import id._
      apiBehavior(idConfig)
    }

    "using Try" should {
      import monadError._
      apiBehavior(tryConfig)
    }

    "using EitherT[Future]" should {
      import monadError._
      apiBehavior(futureConfig)
    }
  }
}