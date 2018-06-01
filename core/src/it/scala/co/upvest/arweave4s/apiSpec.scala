package co.upvest.arweave4s

import com.softwaremill.sttp.{HttpURLConnectionBackend, TryHttpURLConnectionBackend}
import com.softwaremill.sttp.asynchttpclient.future.AsyncHttpClientFutureBackend
import co.upvest.arweave4s.adt.{Block, Transaction, Wallet, Winston}
import co.upvest.arweave4s.utils.BlockchainPatience
import org.scalatest.{Inside, Matchers, WordSpec, Retries}
import org.scalatest.concurrent.{ScalaFutures, Eventually}
import org.scalatest.tagobjects.{Slow, Retryable}
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
  with Eventually with BlockchainPatience with Retries {
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

  override def withFixture(test: NoArgTest) = {
    if (isRetryable(test))
      withRetry { super.withFixture(test) }
    else
      super.withFixture(test)
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
          run { price.estimateForBytes(BigInt(10)) } should be > Winston.Zero
        }

        "return a valid (positive) price for transfer transactions" in {
          run { price.estimateTransfer } should be > Winston.Zero
        }

        "return a price (at least) linear in amount of bytes" taggedAs(Retryable) in {
          val x = randomPositiveBigInt(10000, 0)
          val q = randomPositiveBigInt(100, 0)
          val y = x * q

          val z = run[Winston] { price.estimateForBytes(0) }.amount
          val px = run[Winston] { price.estimateForBytes(x) }.amount
          val py = run[Winston] { price.estimateForBytes(y) }.amount

          (py - z) / (px - z) should be >= q
        }
      }

      "the transaction api" should {

        "submit a transfer transaction" taggedAs(Retryable) in {
          val owner = TestAccount.wallet
          // use our test wallet here since Arweave rejects transactions signed
          // for unknown addresses:
          //   https://github.com/ArweaveTeam/arweave/blob/ed46d7f48c8a22751571eeb541b9fc95e423c243/src/ar_tx.erl#L92
          //   https://github.com/ArweaveTeam/arweave/blob/ed46d7f48c8a22751571eeb541b9fc95e423c243/src/ar_tx.erl#L178

          val extraReward = randomWinstons(upperBound = Winston("1000"))

          val stx = Transaction.Transfer(
            run { address.lastTx(owner) },
            owner,
            Wallet.generate(),
            quantity = randomWinstons(),
            reward = run { price estimateTransfer } + extraReward
          ).sign(owner)

          run[Unit] { tx.submit(stx) } shouldBe (())
        }

        "return a valid transaction by id" taggedAs(Slow, Retryable) in {
          val owner = TestAccount.wallet

          val extraReward = randomWinstons(upperBound = Winston("1000"))
          val stx = Transaction.Transfer(
            run { address.lastTx(owner) },
            owner,
            Wallet.generate(),
            quantity = randomWinstons(upperBound = Winston("100000")),
            reward = run { price estimateTransfer } + extraReward
          ).sign(owner)

          run[Unit] { tx.submit(stx) } shouldBe (())

          eventually {
            inside(run[Transaction.WithStatus]{ tx.get[F, G](stx.id) }) {
              case Transaction.WithStatus.Accepted(t) =>
                t.id shouldBe stx.id
            }
          }
        }

        "transfer ARs to and from a generated wallet" taggedAs(Slow, Retryable) in {
          val initialOwner = TestAccount.wallet
          val intermediateOwner = Wallet.generate()
          val lastOwner = Wallet.generate()

          val quantity1 = Winston.AR
          val quantity2 = randomWinstons(upperBound = Winston("10000000"))

          val extraReward1 = randomWinstons(upperBound = Winston("1000"))
          val extraReward2 = randomWinstons(upperBound = Winston("1000"))

          val stx1 = Transaction.Transfer(
            run { address.lastTx(initialOwner) },
            initialOwner,
            intermediateOwner,
            quantity = quantity1,
            reward = run { price estimateTransfer } + extraReward1
          ).sign(initialOwner)
          run[Unit] { tx submit stx1 } shouldBe (())

          eventually {
            run[Transaction.WithStatus]{ tx.get[F, G](stx1.id) } should
              matchPattern { case Transaction.WithStatus.Accepted(_) => }
          }

          val stx2 = Transaction.Transfer(
            run { address.lastTx(intermediateOwner) },
            intermediateOwner,
            lastOwner,
            quantity = quantity2,
            reward = run { price estimateTransfer } + extraReward2
          ).sign(intermediateOwner)
          run[Unit] { tx submit stx2 } shouldBe (())

          eventually {
            run[Transaction.WithStatus]{ tx.get[F, G](stx2.id) } should
              matchPattern { case Transaction.WithStatus.Accepted(_) => }
          }
        }

        "submit a data transaction" taggedAs(Retryable) in {
          val owner = TestAccount.wallet
          // use our test wallet here since Arweave rejects transactions signed
          // for unknown addresses:
          //   https://github.com/ArweaveTeam/arweave/blob/ed46d7f48c8a22751571eeb541b9fc95e423c243/src/ar_tx.erl#L92
          //   https://github.com/ArweaveTeam/arweave/blob/ed46d7f48c8a22751571eeb541b9fc95e423c243/src/ar_tx.erl#L178

          val data = randomData()

          val extraReward = randomWinstons(upperBound = Winston("1000"))
          val stx = Transaction.Data(
            run { address.lastTx(owner) },
            owner,
            data,
            reward = run { price estimate data } + extraReward
          ).sign(owner)

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
