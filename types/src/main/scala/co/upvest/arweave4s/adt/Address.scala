package co.upvest.arweave4s.adt

import java.security.interfaces.RSAKey
import co.upvest.arweave4s.utils.{CryptoUtils, UnsignedBigInt}
import scala.util.Try

class Address protected (val bytes: Array[Byte]) extends Base64EncodedBytes

object Address {
  final val Length = 32

  def apply(bs: Array[Byte]): Try[Address] = Try { new Address(bs) }

  def fromEncoded(s: String): Option[Address] =
    CryptoUtils.base64UrlDecode(s) map { new Address(_) }

  def ofModulus(n: BigInt):Address =
    new Address(CryptoUtils.sha256(UnsignedBigInt.toBigEndianBytes(n)))

  def ofOwner(o: Owner): Address = ofModulus(o.n)
  def ofKey(k: RSAKey): Address  = ofModulus(k.getModulus)
}
