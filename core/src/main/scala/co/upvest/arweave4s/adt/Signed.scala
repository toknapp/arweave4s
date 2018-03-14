package co.upvest.arweave4s.adt

import java.security.SecureRandom

import org.spongycastle.crypto.digests.SHA256Digest
import org.spongycastle.crypto.engines.RSAEngine
import org.spongycastle.crypto.params.{ParametersWithRandom, RSAPrivateCrtKeyParameters}
import org.spongycastle.crypto.signers.PSSSigner

case class Signed[T](t: T, signature: Signature)

trait Signable {
  def signingData: Array[Byte]
}

object Signable {
  implicit class SignableSyntax[T <: Signable](t: T) {
    def sign(w: Wallet): Signed[T] = {
      val sig_data = t.signingData
  
      val params = new RSAPrivateCrtKeyParameters(
        w.priv.getModulus,
        w.priv.getPublicExponent,
        w.priv.getPrivateExponent,
        w.priv.getPrimeP,
        w.priv.getPrimeQ,
        w.priv.getPrimeExponentP,
        w.priv.getPrimeExponentQ,
        w.priv.getCrtCoefficient
      )
  
      val signer = new PSSSigner(new RSAEngine, new SHA256Digest, 20)
      signer.init(false, new ParametersWithRandom(params, new SecureRandom()))
      signer.update(sig_data, 0, sig_data.length)
      Signed(t, Signature(signer.generateSignature()))
    }
  }
}
