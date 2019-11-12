package co.upvest.arweave4s

import cats.arrow.FunctionK
import cats.data.{EitherT, NonEmptyList}
import cats.implicits._
import cats.{Id, Monad, ~>}
import co.upvest.arweave4s.adt._
import co.upvest.arweave4s.utils.{BlockchainPatience, MultipleHostsBackend}
import com.softwaremill.sttp.asynchttpclient.future.AsyncHttpClientFutureBackend
import com.softwaremill.sttp.{HttpURLConnectionBackend, TryHttpURLConnectionBackend, SttpBackendOptions}
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.tagobjects.{Retryable, Slow}
import org.scalatest.{Inside, Matchers, Retries, WordSpec}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.Try

class apiSpec extends WordSpec
  with Matchers with Inside with ScalaFutures
  with Eventually with BlockchainPatience with Retries
  with RandomValues {

  import ApiTestUtil._
  import api._
  implicit val ec = ExecutionContext.global

  implicit val lift = new (Future ~> EitherT[Future, Failure, ?]) {
    override def apply[A](fa: Future[A]) = EitherT liftF fa
  }

  val idConfig = Config(host = TestHost, HttpURLConnectionBackend())
  val tryConfig = Config(host = TestHost, TryHttpURLConnectionBackend())
  val futConfig = Config(
    host = TestHost,
    AsyncHttpClientFutureBackend(
      SttpBackendOptions.connectionTimeout(
        scaled(10 seconds)
      )
    )
  )
  val eitherTConfig = Backend.lift(futConfig, lift)

  val multiHostBackend = MultipleHostsBackend[EitherT[Future, Failure, ?], Future](
    AsyncHttpClientFutureBackend(),
    NonEmptyList(TestHost, NotExistingTestHost :: Nil),
    MultipleHostsBackend.uniform
  )

  val Some(nonExistentBlock) = ArbitraryInstances.blockIndepHash.arbitrary.sample

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

  def apiBehavior[F[_]: Monad](backend: Backend[F])(
    implicit jh: JsonHandler[F],
    esh: EncodedStringHandler[F],
    sh: SuccessHandler[F],
    run: F ~> Id
  ): Unit = {

      implicit val iBackend: Backend[F] = backend

      "the info api" should {
        "return a valid structure" in {
          run { api.info() } shouldBe an[Info]
        }
      }

      "the peers api" should {
        "return a valid structure" in {
          run { api.peers() } should not be empty
        }
      }

      "the block api" should {
        "return the current block" in {
          run { block.current() } shouldBe a[Block]
        }

        "return the correct block reported as current in the info api" in {
          val i = run { api.info()  }
          val bl = run { block.get(i.current.get) }
          bl.indepHash shouldBe i.current.get
          bl.height shouldBe i.height
        }

        "return a valid block by hash" in {
          val b = run { block.current() }
          run { block.get(b.indepHash) } shouldBe a[Block]
        }

        "return a valid block by height" in {
          val b0 = run { block.get(BigInt(1)) }
          val b1 = run { block.get(b0.indepHash) }
          b0 shouldBe b1
        }

        "fail when a block does not exist (by hash)" in {
          assertThrows[HttpFailure] { run { block.get(nonExistentBlock) } }
        }

        "fail when a block does not exist (by height)" in {
          assertThrows[HttpFailure] { run { block.get(Long.MaxValue) } }
        }

        "return the genesis block" in {
          val g = run { block.get(BigInt(0)) }
          g.previousBlock shouldBe None
          g.isGenesisBlock shouldBe true
        }

        "return a hash list" in {
          val c = run { block.current() }
          val g = run { block.get(BigInt(0)) }

          inside(run { block.hashList[F](c.indepHash) }) { case bs =>
            bs.headOption shouldBe c.previousBlock
            bs.last shouldBe g.indepHash
          }
        }

        "return a map of wallets" in {
          val b = run { block.current() }
          inside(run { block.wallets[F](b.indepHash) }) { case ws =>
            ws should contain key TestAccount.address
          }
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
          val stx = Transaction.transfer(
            run { address.lastTx(owner) },
            owner,
            reward = run { price transferTransactionTo target } + extraReward,
            target = target,
            quantity = randomWinstons(upperBound = Winston("100000"))
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

          val stx1 = Transaction.transfer(
            run { address.lastTx(initialOwner) },
            initialOwner,
            reward = run { price transferTransactionTo intermediateOwner }
              + extraReward1,
            target = intermediateOwner,
            quantity = quantity1
          ).sign(initialOwner)
          run { tx submit stx1 } shouldBe (())

          eventually {
            run { tx.get[F](stx1.id) } should
              matchPattern { case Transaction.WithStatus.Accepted(_) => }
          }

          val stx2 = Transaction.transfer(
            run { address.lastTx(intermediateOwner) },
            intermediateOwner,
            reward = run { price transferTransactionTo lastOwner }
              + extraReward2,
            target = lastOwner,
            quantity = quantity2
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
          val stx = Transaction.data(
            run { address.lastTx(owner) },
            owner,
            run { price dataTransaction data } + extraReward,
            data,
            tags = Nil
          ).sign(owner)

          run { tx.submit(stx) } shouldBe (())

          eventually {
            inside(run { tx.get[F](stx.id) }) {
              case Transaction.WithStatus.Accepted(t) =>
                t.id shouldBe stx.id
                t.data shouldBe Some(data)
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
      apiBehavior(eitherTConfig)
    }

    "using EitherT[Future] and as well an invalid Host" should {
      import monadError._
      apiBehavior(multiHostBackend)
    }
  }
}
