package co.upvest.arweave4s.utils

import org.scalatest.prop.Checkers
import org.scalatest.{WordSpec, Matchers, LoneElement}
import org.scalacheck.Prop.BooleanOperators

class UnsignedBigIntSpec extends WordSpec
  with Matchers with Checkers with LoneElement {
  "UnsignedBigInt" should {
    "encode in big endian" in {
      val bytes = UnsignedBigInt.toBigEndianBytes(
        new BigInt(new java.math.BigInteger("256")))
      val expected = 1.toByte :: 0.toByte :: Nil
      bytes should contain theSameElementsInOrderAs expected
    }

    "decode from big endian bytes" in {
      val bytes = 1.toByte :: 0.toByte :: Nil
      val actual = UnsignedBigInt.ofBigEndianBytes(bytes.toArray)
      val expected = BigInt(256)
      actual shouldBe expected
    }

    "inverse 1" in {
      check { (bi: BigInt) => bi >= 0 ==>
        (UnsignedBigInt.ofBigEndianBytes(
          UnsignedBigInt.toBigEndianBytes(bi)) == bi)
      }
    }

    "inverse 2" in {
      check { (bs: Array[Byte]) => bs.length > 0 ==>
        (UnsignedBigInt.toBigEndianBytes(
          UnsignedBigInt.ofBigEndianBytes(bs)
        ) sameElements bs.dropWhile(_ == 0.toByte))
      }
    }

    "ofBigEndianBytes(Nil)" in {
      pending // TODO: wrap ofBigEndianBytes in Option?
    }

    "toBigEndianBytes(0)" in {
      pending
      UnsignedBigInt.toBigEndianBytes(BigInt(0)).loneElement shouldBe 0.toByte
    }
  }
}
