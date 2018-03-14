package co.upvest.arweave4s.utils

import java.math.BigInteger

object UnsignedBigInt {
  def ofBigEndianBytes(bs: Array[Byte]): BigInt =
    new BigInteger(0.toByte +: bs)

  def toBigEndianBytes(bi: BigInt): Array[Byte] =
    bi.toByteArray drop 1
}
