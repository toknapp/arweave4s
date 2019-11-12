package co.upvest.arweave4s.adt

import co.upvest.arweave4s.marshalling.Marshaller

import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import org.scalatest.{WordSpec, Matchers}
import io.circe.syntax._

class BlockSpec extends WordSpec
  with Matchers with ArbitraryInstances
  with ScalaCheckDrivenPropertyChecks with Marshaller {
  "Block" should {
    "decode its own JSON encoding" in {
      forAll { b: Block =>
        b.asJson.as[Block] should matchPattern { case Right(`b`) => }
      }
    }
  }
}
