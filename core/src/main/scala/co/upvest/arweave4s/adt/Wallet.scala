package co.upvest.arweave4s.adt

import java.math.BigInteger
import java.security.{KeyPairGenerator, SecureRandom}
import java.security.interfaces.{RSAPrivateCrtKey, RSAPublicKey}
import java.security.spec.RSAKeyGenParameterSpec

case class Wallet(pub: RSAPublicKey, priv: RSAPrivateCrtKey) {
  import Wallet._
  require(pub.getPublicExponent == PublicExponent)
  lazy val owner   = Owner(pub.getModulus)
  lazy val address = Address.ofKey(pub)
}

object Wallet {
  final val PublicExponent = new BigInteger("17489")

  def generate(sr: SecureRandom, keySize: Int = 4096): Wallet = {
    val kpg = KeyPairGenerator.getInstance("RSA")
    kpg.initialize(new RSAKeyGenParameterSpec(keySize, PublicExponent), sr)
    val kp = kpg.generateKeyPair()
    Wallet(
      kp.getPublic.asInstanceOf[RSAPublicKey],
      kp.getPrivate.asInstanceOf[RSAPrivateCrtKey]
    )
  }
}
