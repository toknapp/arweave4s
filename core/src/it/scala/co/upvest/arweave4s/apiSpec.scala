package co.upvest.arweave4s

import com.softwaremill.sttp.{HttpURLConnectionBackend, TryHttpURLConnectionBackend}
import com.softwaremill.sttp.asynchttpclient.future.AsyncHttpClientFutureBackend
import co.upvest.arweave4s.adt.{Block, Transaction, Wallet, Winston, Signed}
import co.upvest.arweave4s.utils.BlockchainPatience
import org.scalatest.{Inside, Matchers, WordSpec}
import org.scalatest.concurrent.{ScalaFutures, Eventually}
import org.scalatest.tagobjects.Slow
import cats.{Id, ~>, Monad}
import cats.data.EitherT
import cats.arrow.FunctionK
import cats.instances.try_._
import cats.instances.future._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import scala.util.Try

class apiSpec extends WordSpec
  with Matchers with Inside with ScalaFutures
  with Eventually with BlockchainPatience {
  import ApiTestUtil._
  import api._

  val idConfig = Config(host = TestHost, HttpURLConnectionBackend())
  val tryConfig = Config(host = TestHost, TryHttpURLConnectionBackend())

  implicit val ec = ExecutionContext.global

  val futureConfig = FullConfig[EitherT[Future, Failure, ?], Future](
      host = TestHost,
      AsyncHttpClientFutureBackend(),
      i = new (Future ~> EitherT[Future,Failure, ?]) {
        override def apply[A](fa: Future[A]) = EitherT liftF fa
      }
    )

  val Some(invalidBlockHash) = Block.IndepHash.fromEncoded("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
  val invalidBlockHeight = BigInt(Long.MaxValue)

  val maxReward = Winston.AR

  implicit val idRunner: Id ~> Id = FunctionK.id

  implicit val tryRunner: Try ~> Id = new (Try ~> Id) {
    override def apply[A](fa: Try[A]): A = fa.get
  }


  implicit val eTFRunner = new (λ[α => EitherT[Future, Failure, α]] ~> Id) {
      override def apply[A](fa: EitherT[Future, Failure, A]): A =
        whenReady(fa.value) {
          case Left(e) => throw e
          case Right(a) => a
        }

    }

  def apiBehavior[F[_]: Monad, G[_]](c: AbstractConfig[F, G])(implicit
    jh: JsonHandler[F],
    esh: EncodedStringHandler[F],
    sh: SuccessHandler[F],
    run: F ~> Id): Unit = {

      implicit val _ = c

      "the block api" should {
        "return the current block" in {
          run[Block] { block.current() } shouldBe a[Block]
        }

        "return a valid block by hash" in {
          val b = run[Block] { block.current() }
          run[Block] { block.get(b.indepHash) } shouldBe a[Block]
        }

        "return a valid block by height" in {
          run[Block] { block.get(BigInt(1)) } shouldBe a[Block]
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
          run[Option[Transaction.Id]] { address.lastTx(arbitraryWallet) } shouldBe empty
        }

        "return a positive balance" in {
          run[Winston] { address.balance(TestAccount.address) }
            .amount should be > BigInt(0)
        }

        "return a zero balance" in {
          run[Winston] { address.balance(arbitraryWallet) } shouldBe Winston.Zero

        }
      }

      "tre price api" should {
        "return a valid (positive) price" in {
          run[Winston] { price.estimateForBytes(BigInt(10)) }
            .amount should be > BigInt(0)
        }

        "return a price proportionate in amount of bytes" in {
          val x = randomPositiveBigInt(10000, 0)
          val q = randomPositiveBigInt(100, 0)
          val y = x * q

          val px = run[Winston] { price.estimateForBytes(x) }.amount
          val py = run[Winston] { price.estimateForBytes(y) }.amount

          py / px shouldBe q
        }
      }

      "the transaction api" should {

        "submit a transfer transaction" in {
          val owner = Wallet.generate()

          val utx = Transaction.Transfer(
            Transaction.Id.generate(),
            run { address.lastTx(owner) },
            owner,
            Wallet.generate(),
            quantity = randomWinstons(),
            reward = randomWinstons()
          )

          val stx = utx.copy(reward = run { price estimate utx }).sign(owner)

          run[Unit] { tx.submit(stx) } shouldBe (())
        }

        "eventually return a valid transaction by id" taggedAs(Slow) in {
          val owner = TestAccount.wallet

          val id = Transaction.Id.generate()

          val utx = Transaction.Transfer(
            id,
            run { address.lastTx(owner) },
            owner,
            Wallet.generate(),
            quantity = randomWinstons(upperBound = Winston("100000")),
            reward = maxReward
          )

          val stx = utx.copy(reward = run { price estimate utx }).sign(owner)

          run[Unit] { tx.submit(stx) } shouldBe (())

          val t = eventually {
            inside(run[Transaction.WithStatus]{ tx.get[F, G](id) }) {
              case Transaction.WithStatus.Accepted(Signed(t, _)) => t
            }
          }

          t.id shouldBe id
        }

        "submit a data transaction" in {
          val owner = Wallet.generate()

          val data = randomData()

          val utx = Transaction.Data(
            Transaction.Id.generate(),
            run { address.lastTx(owner) },
            owner,
            data,
            reward = maxReward
          )

          val stx = utx.copy( reward = run { price estimate utx }).sign(owner)

          run[Unit] { tx.submit(stx) } shouldBe (())
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
