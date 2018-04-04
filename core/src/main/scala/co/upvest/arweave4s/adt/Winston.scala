package co.upvest.arweave4s.adt

case class Winston(amount: BigInt) {
  override def toString: String = amount.toString
}

object Winston {
  def apply(s: String): Winston  = new Winston(BigInt(s)) // TODO: this might fail

  val Zero = apply("0")
  val AR = apply("1000000000000")
}
