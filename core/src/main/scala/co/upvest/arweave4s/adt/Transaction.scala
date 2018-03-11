package co.upvest.arweave4s.adt

import java.security.SecureRandom

import co.upvest.arweave4s.adt.Transaction.Type.Type
import org.spongycastle.crypto.digests.SHA256Digest
import org.spongycastle.crypto.engines.RSAEngine
import org.spongycastle.crypto.params.{ParametersWithRandom, RSAPrivateCrtKeyParameters}
import org.spongycastle.crypto.signers.PSSSigner

// Since we want to be as much interoperable with JVM as possible. We gonna to use an abstract class here.
// see this discussion for more: https://stackoverflow.com/questions/1991042/what-is-the-advantage-of-using-abstract-classes-instead-of-traits

sealed abstract class Transaction(id: Id,
                                  lastTx: Option[Id],
                                  owner: Owner,
                                  target: Address,
                                  quantity: Winston,
                                  tpe: Type,
                                  data: Data,
                                  reward: Winston) {

  /**
    * Signing the transaction according to the documentation.
    *
    *
    * unencode  <- Takes input X and returns the completely unencoded form
    * sign      <- Takes data D and key K returns a signature of D signed with K
    *
    * owner     <- unencode(owner)
    * target    <- unencode(target)
    * id        <- unencode(id)
    * data      <- unencode(data)
    * quantity  <- unencode(quantity)
    * reward    <- unencode(reward)
    * last_tx   <- unencode(last_tx)
    *
    * sig_data <- owner + target + id + data + quantity + reward + last_tx
    * signature <- sign(sig_data, key)
    *
    * return signature
    *
    * @param wallet wallet contain private key to sign.
    * @return a Signed transaction.
    *
    */
  def sign(wallet: Wallet): Transaction
}

object Transaction {

  object Type {

    sealed trait Type

    def apply(strRep: String): Type = strRep match {
      case "transfer" => TransferType
      case "data"     => DataType
      case _          => UnknownType
    }

    case object TransferType extends Type {
      override def toString = "transfer"
    }

    case object DataType extends Type {
      override def toString = "data"
    }

    case object UnknownType extends Type {
      override def toString = "unknown"
    }

  }

  case class Signed(id: Id,
                    lastTx: Option[Id],
                    owner: Owner,
                    target: Address,
                    quantity: Winston,
                    tpe: Type,
                    data: Data,
                    signature: Signature,
                    reward: Winston)
      extends Transaction(id: Id,
                          lastTx: Option[Id],
                          owner: Owner,
                          target: Address,
                          quantity: Winston,
                          tpe: Type,
                          data: Data,
                          reward: Winston) {

    /**
      * Signing the transaction according to the documentation.
      *
      *
      * unencode  <- Takes input X and returns the completely unencoded form
      * sign      <- Takes data D and key K returns a signature of D signed with K
      *
      * owner     <- unencode(owner)
      * target    <- unencode(target)
      * id        <- unencode(id)
      * data      <- unencode(data)
      * quantity  <- unencode(quantity)
      * reward    <- unencode(reward)
      * last_tx   <- unencode(last_tx)
      *
      * sig_data <- owner + target + id + data + quantity + reward + last_tx
      * signature <- sign(sig_data, key)
      *
      * return signature
      *
      * @param w wallet contain private key to sign.
      * @return a Signed transaction.
      *
      */
    override def sign(w: Wallet): Signed = this
  }

  case class Unsigned(id: Id,
                      lastTx: Option[Id],
                      owner: Owner,
                      target: Address,
                      quantity: Winston,
                      tpe: Type,
                      data: Data,
                      signature: Signature,
                      reward: Winston)
      extends Transaction(id: Id,
                          lastTx: Option[Id],
                          owner: Owner,
                          target: Address,
                          quantity: Winston,
                          tpe: Type,
                          data: Data,
                          reward: Winston) {

    /**
      * Signing the transaction according to the documentation.
      *
      *
      * unencode  <- Takes input X and returns the completely unencoded form
      * sign      <- Takes data D and key K returns a signature of D signed with K
      *
      * owner     <- unencode(owner)
      * target    <- unencode(target)
      * id        <- unencode(id)
      * data      <- unencode(data)
      * quantity  <- unencode(quantity)
      * reward    <- unencode(reward)
      * last_tx   <- unencode(last_tx)
      *
      * sig_data <- owner + target + id + data + quantity + reward + last_tx
      * signature <- sign(sig_data, key)
      *
      * return signature
      *
      * @param w wallet contain private key to sign.
      * @return a Signed transaction.
      *
      */
    override def sign(w: Wallet): Signed = {
      val sig_data =
        Array.concat(
          owner.bytes,
          target.bytes,
          id.bytes,
          data.bytes,
          quantity.toString.getBytes,
          reward.toString.getBytes,
          lastTx map {
            _.bytes
          } getOrElse Array.empty[Byte]
        )

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
      val sig = Signature(signer.generateSignature())
      this.asInstanceOf[Signed].copy(signature = sig)
    }
  }

}
