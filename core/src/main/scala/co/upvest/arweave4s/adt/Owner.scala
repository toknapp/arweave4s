package co.upvest.arweave4s.adt

import java.security.KeyFactory
import java.security.interfaces.RSAPublicKey
import java.security.spec.RSAPublicKeySpec

import co.upvest.arweave4s.utils.{CryptoUtils, UnsignedBigInt}

case class Owner(n: BigInt) {
  lazy val bytes = UnsignedBigInt.toBigEndianBytes(n)
  override def toString(): String = CryptoUtils.base64UrlEncode(bytes)
  lazy val publicKey: RSAPublicKey =
    KeyFactory.getInstance("RSA").generatePublic(
      new RSAPublicKeySpec(n.bigInteger, Wallet.PublicExponentUsedByArweave)
    ).asInstanceOf[RSAPublicKey]
}

case object Owner {
  def fromEncoded(s: String) =
    Owner(UnsignedBigInt.ofBigEndianBytes(CryptoUtils.base64UrlDecode(s)))

  implicit def ownerToPublicKey(o: Owner): RSAPublicKey = o.publicKey
}
