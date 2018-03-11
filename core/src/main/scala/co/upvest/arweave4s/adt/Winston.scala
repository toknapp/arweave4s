package co.upvest.arweave4s.adt

case class Winston(amount: BigInt) {
  override def toString: String = amount.toString
}
