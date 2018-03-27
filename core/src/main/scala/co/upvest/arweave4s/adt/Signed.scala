package co.upvest.arweave4s.adt

import java.security.SecureRandom
import java.security.interfaces.{RSAPrivateCrtKey, RSAPublicKey}

import org.spongycastle.crypto.digests.SHA256Digest
import org.spongycastle.crypto.engines.RSAEngine
import org.spongycastle.crypto.params.{ParametersWithRandom, RSAPrivateCrtKeyParameters, RSAKeyParameters}
import org.spongycastle.crypto.signers.PSSSigner

case class Signed[+T](t: T, signature: Signature)

trait Signable {
  def signingData: Array[Byte]
}

object Signable {
  implicit class SignableSyntax[T <: Signable](t: T) {
    def sign(priv: RSAPrivateCrtKey): Signed[T] = {
      val sig_data = t.signingData

      val params = new RSAPrivateCrtKeyParameters(
        priv.getModulus,
        priv.getPublicExponent,
        priv.getPrivateExponent,
        priv.getPrimeP,
        priv.getPrimeQ,
        priv.getPrimeExponentP,
        priv.getPrimeExponentQ,
        priv.getCrtCoefficient
      )

      val signer = new PSSSigner(new RSAEngine, new SHA256Digest, 20)
      signer.init(false, new ParametersWithRandom(params, new SecureRandom()))
      signer.update(sig_data, 0, sig_data.length)
      Signed(t, new Signature(signer.generateSignature()))
    }
  }

  implicit class VerifiableSyntax[T <: Signable](st: Signed[T]) {
    def verify(pub: RSAPublicKey): Boolean = {
      val sig_data = st.signingData

      val signer = new PSSSigner(new RSAEngine, new SHA256Digest, 20)
      signer.init(
        true,
        new RSAKeyParameters(false, pub.getModulus, pub.getPublicExponent)
      )
      signer.update(sig_data, 0, sig_data.length)
      signer.verifySignature(st.signature.bytes)
    }
  }

  implicit def castSignable[T](st: Signed[T]): T = st.t
}
