package co.upvest.arweave4s.utils

import org.scalatest.{WordSpec, Matchers}

class UnsignedBigIntSpec extends WordSpec with Matchers {
  "UnsignedBigInt" should {
    "encode in big endian" in {
      val bytes = UnsignedBigInt.toBigEndianBytes(BigInt(256))
      val expected = 1.toByte :: 0.toByte :: Nil
      bytes should contain theSameElementsInOrderAs expected
    }

    "decode from big endian bytes" in {
      val bytes = 1.toByte :: 0.toByte :: Nil
      val actual = UnsignedBigInt.ofBigEndianBytes(bytes.toArray)
      val expected = BigInt(256)
      actual shouldBe expected
    }
  }
}
