package co.upvest.arweave4s.adt

case class Winston(amount: BigInt) {
  override def toString: String = amount.toString

  def plus(o: Winston): Winston = Winston(amount + o.amount)
}

object Winston {
  def apply(bi: BigInt): Winston = new Winston(bi)
  def apply(s: String): Winston  = new Winston(BigInt(s)) // TODO: this might fail

  val Zero = apply("0")
  val AR = apply("1000000000000")
}
