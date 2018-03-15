package co.upvest.arweave4s.adt

import java.security.interfaces.RSAKey

import co.upvest.arweave4s.utils.{CryptoUtils, UnsignedBigInt}

class Address(val bytes: Array[Byte]) extends Base64EncodedBytes

object Address {
  def fromEncoded(s: String): Option[Address] =
    CryptoUtils.base64UrlDecode(s) map { new Address(_) }

  def ofModulus(n: BigInt) =
    new Address(CryptoUtils.sha256(UnsignedBigInt.toBigEndianBytes(n)))

  def ofOwner(o: Owner): Address = ofModulus(o.n)
  def ofKey(k: RSAKey): Address  = ofModulus(k.getModulus)
}
