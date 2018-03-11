package co.upvest.arweave4s.adt

import java.security.interfaces.RSAKey

import co.upvest.arweave4s.utils.CryptoUtils

case class Address(bytes: Array[Byte]) {
  override def toString(): String = Id(bytes).toString
}

object Address {

  def apply(base64urlEncoded: String): Address = Address(
    CryptoUtils.base64UrlDecode(base64urlEncoded)
  )
  // NB. drop 1 to discard the 2s complement bit
  def ofModulus(n: BigInt) =
    Address(CryptoUtils.sha256(n.toByteArray drop 1))

  def ofOwner(o: Owner): Address = ofModulus(o.n)
  def ofKey(k: RSAKey): Address  = ofModulus(k.getModulus)
}
