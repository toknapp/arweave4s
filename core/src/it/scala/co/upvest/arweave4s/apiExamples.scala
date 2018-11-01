package co.upvest.arweave4s

import java.util.concurrent.Executors

import cats.Id
import cats.instances.future._
import co.upvest.arweave4s.adt._
import co.upvest.arweave4s.utils.BlockchainPatience
import com.softwaremill.sttp.HttpURLConnectionBackend
import com.softwaremill.sttp.asynchttpclient.future.AsyncHttpClientFutureBackend
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.tagobjects.{Retryable, Slow}
import org.scalatest.{Tag => _, _}

import scala.concurrent.{ExecutionContext, Future}

class apiExamples extends WordSpec
  with Matchers with GivenWhenThen with Eventually
  with BlockchainPatience with Retries with LoneElement
  with Inside with ScalaFutures with RandomValues {
  import ApiTestUtil._

  override def withFixture(test: NoArgTest) = {
    if (isRetryable(test))
      withRetry { super.withFixture(test) }
    else
      super.withFixture(test)
  }

  "An api axample" should {
    "be able to use Id" taggedAs(Slow, Retryable) in {
      implicit val c = api.Config(host = TestHost, HttpURLConnectionBackend())
      import api.id._
      Given("an amount of Winstons to transfer")
      val quantity = Winston("100000")

      And("a wallet")
      val wallet: Wallet = TestAccount.wallet
      And("that it has enough funds in it")
      val reward = randomWinstons()
      val requiredFunds = reward + quantity
      api.address.balance(wallet) should be >= requiredFunds
      Given("a freshly generated wallet")
      val beneficiary = Wallet.generate()

      Then("it should not have any Winstons")
      api.address.balance(beneficiary) shouldBe Winston.Zero

      When("a transfer is submitted")
      val lastTx = api.address.lastTx[Id](wallet) // TODO: why don't type-inference work here?
      val stx = Transaction.transfer(
        lastTx,
        wallet,
        reward = reward,
        target = beneficiary,
        quantity = quantity
      ).sign(wallet)

      api.tx.submit(stx)

      Then("the new wallet should have received the Winstons")
      eventually {
        api.address.balance(beneficiary) shouldBe quantity
      }

      And("the transaction id should be in their transaction histories")
      api.arql(
        Query.transactionHistory(wallet)
      ).toList should contain (stx.id)

      api.arql(
        Query.transactionHistory(beneficiary)
      ).toList.loneElement shouldBe stx.id
    }

    "be able to use for-comprehensions" taggedAs(Retryable) in {

      implicit val c = api.Config(host = TestHost, AsyncHttpClientFutureBackend())
      import api.future._

      implicit val ec = apiExamples.ec

      Given("some test data that will last forever and a tag to find it")
      val testData = Data("Hi Mom!".getBytes("UTF-8"))
      val tag = Tag.Custom(
        name = randomBytes(12),
        value = randomBytes(24)
      )

      And("a wallet")
      val wallet = TestAccount.wallet

      Then("a transaction should be successful")

      val f = for {
        price    <- api.price.dataTransaction(testData)
        lastTx   <- api.address.lastTx(wallet)
        stx = Transaction.data(
          lastTx = lastTx,
          owner  = wallet,
          data   = testData,
          reward = price,
          tags   = tag :: Nil
        ).sign(wallet)
        ()       <- api.tx.submit(stx)
      } yield stx


      whenReady(f) { stx =>
        And("eventually get accepted")
        eventually {
          whenReady(api.tx.get[Future](stx.id)) { ts =>
            inside(ts) {
              case Transaction.WithStatus.Accepted(t) =>
                t.id shouldBe stx.id
                t.data shouldBe Some(testData)
            }
          }
        }

        Then("it should be findable with the tag")
        whenReady(api.arql(Query.transactionsWithTag(tag))) { txs =>
          txs.toList.loneElement shouldBe stx.id
        }
      }
    }
  }
}

object apiExamples {
  implicit lazy val ec:ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(2))
}


