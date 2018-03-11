package co.upvest.arweave4s.adt

import java.security.SecureRandom

import co.upvest.arweave4s.utils.CryptoUtils

class Id(val bytes: Array[Byte]) {

  override def toString: String = CryptoUtils.base64UrlEncode(bytes)

  override def equals(that: Any): Boolean = that match {
    case id: Id => id.bytes sameElements bytes
    case _      => false
  }

  override def hashCode(): Int = bytes.hashCode()

}

object Id {

  final val Length = 32

  def generate(size: Int = Length, sr: SecureRandom = new SecureRandom()) = {
    val repr = new Array[Byte](size)
    sr.nextBytes(repr)
    Id(repr)
  }

  def fromB64urlEncoded(s: String): Id = Id(CryptoUtils.base64UrlDecode(s))

  // From decoded bytes.
  def apply(a: Array[Byte]): Id = {
    require(a.length <= Length, s"Invalid hash id byte size with: $a")
    new Id(a)
  }
}
