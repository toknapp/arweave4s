package co.upvest.arweave4s.utils

import org.scalatest.prop.Checkers
import org.scalatest.{WordSpec, Matchers, LoneElement}
import org.scalacheck.Prop.BooleanOperators

class UnsignedBigIntSpec extends WordSpec
  with Matchers with Checkers with LoneElement {
  "UnsignedBigInt" should {
    "encode in big endian" in {
      val bytes = UnsignedBigInt.toBigEndianBytes(BigInt("256"))
      val expected = 1.toByte :: 0.toByte :: Nil
      bytes should contain theSameElementsInOrderAs expected
    }

    "decode from big endian bytes" in {
      val bytes = 1.toByte :: 0.toByte :: Nil
      UnsignedBigInt.ofBigEndianBytes(bytes.toArray) shouldBe Some(BigInt(256))
    }

    "inverse 1" in {
      check { (bi: BigInt) => bi >= 0 ==>
        (UnsignedBigInt.ofBigEndianBytes(
          UnsignedBigInt.toBigEndianBytes(bi)).get == bi)
      }
    }

    def trimLeadingZeroes(bs: Array[Byte]): Array[Byte] =
      bs.dropWhile(_ == 0.toByte)

    "inverse 2" in {
      check { (bs: Array[Byte]) => (trimLeadingZeroes(bs).length > 0) ==>
        (UnsignedBigInt.toBigEndianBytes(
          UnsignedBigInt.ofBigEndianBytes(bs).get
        ) sameElements trimLeadingZeroes(bs))
      }
    }

    "ofBigEndianBytes(Nil)" in {
      UnsignedBigInt.ofBigEndianBytes(Array.empty) shouldBe None
    }

    "ofBigEndianBytes(0 :: Nil)" in {
      UnsignedBigInt.ofBigEndianBytes(Array(0.toByte)) shouldBe Some(BigInt(0))
    }

    "toBigEndianBytes(0)" in {
      UnsignedBigInt.toBigEndianBytes(BigInt(0)).loneElement shouldBe 0.toByte
    }
  }
}
