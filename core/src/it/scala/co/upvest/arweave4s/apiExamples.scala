package co.upvest.arweave4s

import com.softwaremill.sttp.HttpURLConnectionBackend
import co.upvest.arweave4s.adt.{Wallet, Winston, Transaction}
import co.upvest.arweave4s.utils.BlockchainPatience
import org.scalatest.{WordSpec, Matchers, GivenWhenThen}
import org.scalatest.concurrent.Eventually
import org.scalatest.tagobjects.Slow

import cats.Id

class apiExamples extends WordSpec
  with Matchers with GivenWhenThen with Eventually with BlockchainPatience {
  import ApiTestUtil._

  "An api axample" should {
    "be able to use Id" taggedAs(Slow) in {
      val wallet: Wallet = TestAccount.wallet

      implicit val c = api.Config(host = TestHost, HttpURLConnectionBackend())
      import api.id._

      val beneficiary = Wallet.generate()

      api.address.balance(beneficiary) shouldBe Winston.Zero

      val quantity = Winston("100000")

      val lastTx = api.address.lastTx[Id, Id](wallet) // TODO: why don't type-inference work here?

      val stx = Transaction.Transfer(
        Transaction.Id.generate(),
        lastTx,
        wallet,
        beneficiary,
        quantity = quantity,
        reward = randomWinstons()
      ).sign(wallet)

      api.tx.submit(stx)

      eventually {
        api.address.balance(beneficiary) shouldBe quantity
      }
    }
  }
}
