package co.upvest.arweave4s.adt

import co.upvest.arweave4s.utils.CryptoUtils

final case class Transaction(
  lastTx: Option[Transaction.Id],
  owner: Owner,
  reward: Winston,
  data: Option[Base64EncodedBytes],
  tags: Option[Seq[Tag.Custom]],
  target: Option[Address],
  quantity: Winston,
) extends Signable {
  lazy val signingData = Array.concat(
    owner.bytes,
    target map { _.bytes } getOrElse Array.empty,
    data map { _.bytes } getOrElse Array.empty,
    quantity.toString.getBytes,
    reward.toString.getBytes,
    lastTx map { _.bytes } getOrElse Array.empty,
    tags.toSeq.flatten.map { t => t.name ++ t.value }.flatten.toArray
  )

  def withData(d: Array[Byte]) = this.copy(data = Some(Data(d)))
}

object Transaction {

  class Id protected[Transaction] (val bytes: Array[Byte]) extends Base64EncodedBytes

  object Id {
    final val Length = 32

    def fromEncoded(s: String): Option[Id] =
      CryptoUtils.base64UrlDecode(s) map { new Id(_) }
  }

  def data(
    lastTx: Option[Id],
    owner: Owner,
    reward: Winston,
    data: Base64EncodedBytes,
    tags: Seq[Tag.Custom]
  ): Transaction = Transaction(
    lastTx,
    owner,
    reward,
    Some(data),
    Some(tags),
    None,
    Winston.Zero
  )

  def transfer(
    lastTx: Option[Id],
    owner: Owner,
    reward: Winston,
    target: Address,
    quantity: Winston,
  ): Transaction = Transaction(
    lastTx,
    owner,
    reward,
    None,
    None,
    Some(target),
    quantity
  )

  sealed trait WithStatus
  object WithStatus {
    case class NotFound(id: Id) extends WithStatus
    case class Pending(id: Id) extends WithStatus
    case class Gone(id: Id) extends WithStatus
    case class Accepted(stx: Signed[Transaction]) extends WithStatus
  }

  implicit class SignedTransaction(stx: Signed[Transaction]) {
    def id: Id = new Id(CryptoUtils.sha256(stx.signature.bytes))
  }
}
