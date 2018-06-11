package co.upvest.arweave4s.adt

import org.scalatest.prop.{Checkers, GeneratorDrivenPropertyChecks}
import org.scalatest.{WordSpec, Matchers}

class Base64EncodedBytesSpec extends WordSpec with Matchers with Checkers with GeneratorDrivenPropertyChecks {
  class Foo(val bytes: Array[Byte]) extends Base64EncodedBytes

  "Base64EncodedBytes" should {
    "support equality" in {
      forAll { (a: Array[Byte]) =>
        new Foo(a) shouldBe new Foo(a.clone)
      }
    }

    "support inequality" in {
      forAll { (a: Array[Byte], b: Array[Byte]) =>
        whenever(a.deep != b.deep) {
          new Foo(a) should not be (new Foo(b))
        }
      }
    }

    "equality should imply same hashCodes " in {
      forAll { (a: Array[Byte]) =>
        (new Foo(a)).hashCode shouldBe (new Foo(a.clone)).hashCode
      }
    }
  }
}
