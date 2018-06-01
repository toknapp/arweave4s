package co.upvest.arweave4s.adt

import co.upvest.arweave4s.utils.CryptoUtils

trait Transaction extends Signable {
  def lastTx: Option[Transaction.Id]
  def owner: Owner
  def reward: Winston
  def tpe: Transaction.Type
}

object Transaction {

  class Id protected[Transaction] (val bytes: Array[Byte]) extends Base64EncodedBytes

  object Id {
    final val Length = 32

    def fromEncoded(s: String): Option[Id] =
      CryptoUtils.base64UrlDecode(s) map { new Id(_) }
  }

  sealed trait Type
  object Type {
    case object Transfer extends Type
    case object Data extends Type
  }

  /**
    * Signing the transaction according to the documentation.
    *
    * unencode  <- Takes input X and returns the completely unencoded form
    * sign      <- Takes data D and key K returns a signature of D signed with K
    *
    * owner     <- unencode(owner)
    * target    <- unencode(target)
    * data      <- unencode(data)
    * quantity  <- unencode(quantity)
    * reward    <- unencode(reward)
    * last_tx   <- unencode(last_tx)
    *
    * sig_data <- owner + target + data + quantity + reward + last_tx
    * signature <- sign(sig_data, key)
    *
    */
  case class Data(lastTx: Option[Id], owner: Owner, data: Base64EncodedBytes, reward: Winston, tags: Seq[Tag.Custom]) extends Transaction {
    val tpe: Type = Type.Data
    lazy val signingData = Array.concat(
      owner.bytes,
      Array.empty,
      data.bytes,
      Winston.Zero.toString.getBytes,
      reward.toString.getBytes,
      lastTx map { _.bytes } getOrElse Array.empty
    )
  }

  case class Transfer(lastTx: Option[Id], owner: Owner, target: Address, quantity: Winston, reward: Winston) extends Transaction {
    val tpe: Type = Type.Transfer
    lazy val signingData = Array.concat(
      owner.bytes,
      target.bytes,
      Array.empty,
      quantity.toString.getBytes,
      reward.toString.getBytes,
      lastTx map { _.bytes } getOrElse Array.empty
    )
  }

  sealed trait WithStatus
  object WithStatus {
    case class NotFound(id: Id) extends WithStatus
    case class Pending(id: Id) extends WithStatus
    case class Accepted(stx: Signed[Transaction]) extends WithStatus
  }

  implicit class SignedTransaction[T <: Transaction](stx: Signed[T]) {
    def id: Id = new Id(CryptoUtils.sha256(stx.signature.bytes))
  }
}
