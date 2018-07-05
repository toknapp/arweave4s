package co.upvest.arweave4s

import com.softwaremill.sttp.{HttpURLConnectionBackend, TryHttpURLConnectionBackend}
import com.softwaremill.sttp.asynchttpclient.future.AsyncHttpClientFutureBackend
import co.upvest.arweave4s.adt.{Block, Info, Transaction, Wallet, Winston}
import co.upvest.arweave4s.utils.{BlockchainPatience, MultipleHostsBackend}
import org.scalatest.{Inside, Matchers, Retries, WordSpec}
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.tagobjects.{Retryable, Slow}
import cats.{Id, Monad, ~>}
import cats.data.{EitherT, NonEmptyList}
import cats.arrow.FunctionK
import cats.implicits._
import com.softwaremill.sttp.UriContext

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class apiSpec extends WordSpec
  with Matchers with Inside with ScalaFutures
  with Eventually with BlockchainPatience with Retries {
  import ApiTestUtil._
  import api._
  implicit val ec = ExecutionContext.global

  implicit val lift = new (Future ~> EitherT[Future, Failure, ?]) {
    override def apply[A](fa: Future[A]) = EitherT liftF fa
  }

  val idConfig = Config(host = uri"$TestHost", HttpURLConnectionBackend())
  val tryConfig = Config(host = uri"$TestHost", TryHttpURLConnectionBackend())
  val futConfig = Config(host = uri"$TestHost", AsyncHttpClientFutureBackend())

  val futureConfig = AdvancedConfig[EitherT[Future, Failure, ?], Future](
    futConfig,
    i = lift
  )

  val multiHostBackend = new MultipleHostsBackend[EitherT[Future, Failure, ?], Future](
    AsyncHttpClientFutureBackend(),
    NonEmptyList(uri"$TestHost", uri"$NotExistingTestHost" :: Nil),
    MultipleHostsBackend.uniform
  )

  val Some(invalidBlockHash) = Block.IndepHash.fromEncoded("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
  val invalidBlockHeight = BigInt(Long.MaxValue)

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

  def apiBehavior[F[_]: Monad](c: Backend[F])(implicit
                                                       jh: JsonHandler[F],
                                                       esh: EncodedStringHandler[F],
                                                       sh: SuccessHandler[F],
                                                       run: F ~> Id): Unit = {

      implicit val _ = c

      "the info api" should {
        "return a valid structure" in {
          run { api.info() } shouldBe an[Info]
        }
      }

      "the block api" should {
        "return the current block" in {
          run { block.current() } shouldBe a[Block]
        }

        "return a valid block by hash" in {
          val b = run { block.current() }
          run { block.get(b.indepHash) } shouldBe a[Block]
        }

        "return a valid block by height" in {
          run { block.get(BigInt(1)) } shouldBe a[Block]
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
          run { address.balance(TestAccount.address) } should be > Winston.Zero
        }

        "return a zero balance" in {
          run { address.balance(arbitraryWallet) } shouldBe Winston.Zero

        }
      }

      "the price api" should {
        val oldAddress = TestAccount.address
        val newAddress = Wallet.generate().address

        "return a valid (positive) price for a data transaction" in {
          run { price.dataTransaction(randomData()) } should be > Winston.Zero
        }

        "return a valid (positive) price for transfer transactions (for an existing address)" in {
          run {
            price.transferTransactionTo(oldAddress)
          } should be > Winston.Zero
        }

        "return a valid (positive) price for transfer transactions (for a new address)" in {
          run {
            price.transferTransactionTo(newAddress)
          } should be > Winston.Zero
        }

        "transfering to an existing wallet should have a lower price" in {
          run { address.balance(oldAddress) } should be > Winston.Zero

          run { address.balance(newAddress) } shouldBe Winston.Zero

          val o = run { price.transferTransactionTo(oldAddress) }
          val n = run { price.transferTransactionTo(newAddress) }
          o should be < n
        }

        "return a deterministic price" taggedAs(Retryable) in {
          val d = randomData()
          val p = run { price.dataTransaction(d) }
          val q = run { price.dataTransaction(d) }

          p shouldBe q
        }
      }

      "the transaction api" should {

        "submit a valid transfer transaction" taggedAs(Slow, Retryable) in {
          val owner = TestAccount.wallet

          val extraReward = randomWinstons(upperBound = Winston("1000"))
          val target = Wallet.generate()
          val stx = Transaction.Transfer(
            run { address.lastTx(owner) },
            owner,
            target,
            quantity = randomWinstons(upperBound = Winston("100000")),
            reward = run { price transferTransactionTo target } + extraReward
          ).sign(owner)

          run { tx.submit(stx) } shouldBe (())

          eventually {
            inside(run { tx.get[F](stx.id) }) {
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
            reward = run { price transferTransactionTo intermediateOwner }
              + extraReward1
          ).sign(initialOwner)
          run { tx submit stx1 } shouldBe (())

          eventually {
            run { tx.get[F](stx1.id) } should
              matchPattern { case Transaction.WithStatus.Accepted(_) => }
          }

          val stx2 = Transaction.Transfer(
            run { address.lastTx(intermediateOwner) },
            intermediateOwner,
            lastOwner,
            quantity = quantity2,
            reward = run { price transferTransactionTo lastOwner }
              + extraReward2
          ).sign(intermediateOwner)
          run { tx submit stx2 } shouldBe (())

          eventually {
            run { tx.get[F](stx2.id) } should
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
            reward = run { price dataTransaction data } + extraReward,
            tags = Nil
          ).sign(owner)

          run { tx.submit(stx) } shouldBe (())

          waitForDataTransaction(stx)

          eventually {
            inside(run { tx.get[F](stx.id) }) {
              case Transaction.WithStatus.Accepted(t) =>
                t.id shouldBe stx.id
            }
          }
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

    "using EitherT[Future] and as well an invalid Host" should {
      import monadError._
      apiBehavior(multiHostBackend)
    }
  }
}
