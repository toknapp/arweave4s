package co.upvest.arweave4s.adt

import co.upvest.arweave4s.utils.{CryptoUtils, UnsignedBigInt}

case class Owner(n: BigInt) {
  lazy val bytes = UnsignedBigInt.toBigEndianBytes(n)
  override def toString(): String = CryptoUtils.base64UrlEncode(bytes)
}

case object Owner {
  def fromEncoded(s: String) =
    Owner(BigInt.apply(CryptoUtils.base64UrlDecode(s)))
}
