package co.upvest.arweave4s.adt

import java.math.BigInteger
import java.security.{KeyPairGenerator, SecureRandom}
import java.security.interfaces.{RSAPrivateCrtKey, RSAPublicKey}
import java.security.spec.RSAKeyGenParameterSpec

case class Wallet(pub: RSAPublicKey, priv: RSAPrivateCrtKey) {
  require(pub.getPublicExponent == Wallet.PublicExponentUsedByArweave)
  lazy val owner   = Owner(pub.getModulus)
  lazy val address = Address.ofKey(pub)
}

object Wallet {
  // at the time of writing the following public exponent is enforced, see:
  // - https://github.com/ArweaveTeam/arweave/blob/18a7aeafa97b54a444ca53fadaf9c94b6075a87c/src/ar_wallet.erl#L74
  // - https://github.com/ArweaveTeam/arweave/blob/18a7aeafa97b54a444ca53fadaf9c94b6075a87c/src/ar_wallet.erl#L3
  final val PublicExponentUsedByArweave = new BigInteger("17489")

  def generate(sr: SecureRandom, keySize: Int = 4096): Wallet = {
    val kpg = KeyPairGenerator.getInstance("RSA")
    kpg.initialize(
      new RSAKeyGenParameterSpec(keySize, PublicExponentUsedByArweave),
      sr
    )
    val kp = kpg.generateKeyPair()
    Wallet(
      kp.getPublic.asInstanceOf[RSAPublicKey],
      kp.getPrivate.asInstanceOf[RSAPrivateCrtKey]
    )
  }

  implicit def walletToPublicKey(w: Wallet): RSAPublicKey = w.pub
  implicit def walletToPrivateKey(w: Wallet): RSAPrivateCrtKey = w.priv
}
