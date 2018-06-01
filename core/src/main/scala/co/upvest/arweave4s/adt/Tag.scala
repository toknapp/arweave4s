package co.upvest.arweave4s.adt

sealed trait Tag

object Tag {
  case class Custom(name: Array[Byte], value: Array[Byte]) extends Tag
  case class To(address: Address) extends Tag
  case class From(address: Address) extends Tag
}
