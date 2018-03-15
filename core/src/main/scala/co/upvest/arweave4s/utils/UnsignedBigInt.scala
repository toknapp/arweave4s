package co.upvest.arweave4s.utils

object UnsignedBigInt {
  def ofBigEndianBytes(bs: Array[Byte]): Option[BigInt] =
    if (bs.isEmpty) None else Some(BigInt(0.toByte +: bs))

  def toBigEndianBytes(bi: BigInt): Array[Byte] = {
    val bs = bi.toByteArray
    if (bs.length > 1 && bs.head == 0.toByte) bs.tail else bs
  }
}
