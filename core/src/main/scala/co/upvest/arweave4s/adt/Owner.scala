package co.upvest.arweave4s.adt

import co.upvest.arweave4s.utils.CryptoUtils

case class Owner(n: BigInt) {
  lazy val bytes                  = n.toByteArray drop 1
  override def toString(): String = CryptoUtils.base64UrlEncode(bytes)
}

case object Owner {
  def fromB64UrlEncoded(s: String) = Owner(BigInt.apply(CryptoUtils.base64UrlDecode(s)))
}
