package co.upvest.arweave4s.adt

case class Winston(amount: BigInt) {
  override def toString: String = amount.toString
}

object Winston {
  def apply(bi: BigInt): Winston = new Winston(bi)
  def apply(s: String): Winston = new Winston(BigInt(s))
}
