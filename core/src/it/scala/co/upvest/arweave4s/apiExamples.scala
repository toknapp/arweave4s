package co.upvest.arweave4s

import java.util.concurrent.Executors

import com.softwaremill.sttp.HttpURLConnectionBackend
import co.upvest.arweave4s.adt.{Data, Transaction, Wallet, Winston}
import co.upvest.arweave4s.utils.BlockchainPatience
import org.scalatest.{GivenWhenThen, Matchers, WordSpec}
import org.scalatest.concurrent.Eventually
import org.scalatest.tagobjects.Slow
import cats.Id
import com.softwaremill.sttp.asynchttpclient.future.AsyncHttpClientFutureBackend

import scala.concurrent.ExecutionContext

class apiExamples extends WordSpec
  with Matchers with GivenWhenThen with Eventually with BlockchainPatience {
  import ApiTestUtil._


  "An api axample" should {
    "be able to use Id" taggedAs(Slow) in {

      implicit val c = api.Config(host = TestHost, HttpURLConnectionBackend())
      import api.id._
      Given("an amount of Winstons to transfer")
      val quantity = Winston("100000")

      And("a wallet")
      val wallet: Wallet = TestAccount.wallet
      And("that it has enough funds in it")
      val reward = randomWinstons()
      // TODO: val requiredFunds = reward + quantity
      //       api.address.balance(wallet) should be >= requiredFunds
      val requiredFunds = Winston(reward.amount + quantity.amount)
      api.address.balance(wallet).amount should be >= requiredFunds.amount

      Given("a freshly generated wallet")
      val beneficiary = Wallet.generate()

      Then("it should not have any Winstons")
      api.address.balance(beneficiary) shouldBe Winston.Zero

      When("a transfer is submitted")
      val lastTx = api.address.lastTx[Id, Id](wallet) // TODO: why don't type-inference work here?
      val stx = Transaction.Transfer(
        Transaction.Id.generate(),
        lastTx,
        wallet,
        beneficiary,
        quantity = quantity,
        reward = reward
      ).sign(wallet)

      api.tx.submit(stx)

      Then("the new wallet should have received the Winstons")
      eventually {
        api.address.balance(beneficiary) shouldBe quantity
      }
    }

    "be able to use for-comprehensions" in {

      implicit val c = api.Config(host = TestHost, AsyncHttpClientFutureBackend())
      import api.future._

      implicit val ec = apiExamples.ec



      Given("some test data that will last forever")

      val testData = Data("Hi Mom!".getBytes("UTF-8"))

      And("a wallet")
      val wallet = TestAccount.wallet

      Then("a transaction should be successful")

      for {
        price    <- api.price.estimate(testData)
        lastTx   <- api.address.lastTx(wallet)
        ()       <- api.tx.submit(
          Transaction.Data(
            id     = Transaction.Id.generate(),
            lastTx = lastTx,
            owner  = wallet,
            data   = testData,
            reward = price
        ).sign(wallet))
      } yield ()
    }
  }
}

object apiExamples {
  implicit lazy val ec:ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(2))
}


