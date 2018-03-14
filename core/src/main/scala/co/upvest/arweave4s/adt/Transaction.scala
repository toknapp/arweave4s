package co.upvest.arweave4s.adt

import java.security.SecureRandom

import co.upvest.arweave4s.utils.CryptoUtils

trait Transaction {
  def id: Transaction.Id
  def lastTx: Option[Transaction.Id]
  def owner: Owner
  def reward: Winston
  def tpe: Transaction.Type
}

object Transaction {

  class Id(val bytes: Array[Byte]) extends Base64EncodedBytes

  object Id {
    final val Length = 32

    def generate(size: Int = Length, sr: SecureRandom = new SecureRandom()) = {
      val repr = new Array[Byte](size)
      sr.nextBytes(repr)
      new Id(repr)
    }

    def fromEncoded(s: String): Id = new Id(CryptoUtils.base64UrlDecode(s))
  }

  sealed trait Type

  object Type {

    def apply(strRep: String): Option[Type] = strRep match {
      case "transfer" => Some(Transfer)
      case "data"     => Some(Data)
      case _          => None
    }

    case object Transfer extends Type {
      override def toString = "transfer"
    }

    case object Data extends Type {
      override def toString = "data"
    }
  }

 /**
   * Signing the transaction according to the documentation.
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
   */

  case class Data(
    id: Id,
    lastTx: Option[Id],
    owner: Owner,
    data: Base64EncodedBytes,
    reward: Winston) extends Transaction with Signable {
      val tpe: Type = Type.Data
      val signingData = Array.concat(
        owner.bytes,
        Array.empty,
        id.bytes,
        data.bytes,
        Array.empty,
        reward.toString.getBytes,
        lastTx map { _.bytes } getOrElse Array.empty[Byte]
      )
    }

  case class Transfer(
    id: Id,
    lastTx: Option[Id],
    owner: Owner,
    target: Address,
    quantity: Winston,
    reward: Winston) extends Transaction with Signable {
      val tpe: Type = Type.Transfer
      val signingData = Array.concat(
        owner.bytes,
        target.bytes,
        id.bytes,
        Array.empty,
        quantity.toString.getBytes,
        reward.toString.getBytes,
        lastTx map { _.bytes } getOrElse Array.empty[Byte]
      )
    }

}
