package co.upvest.arweave4s.adt

sealed trait Query
object Query {
  case class And(q1: Query, q2: Query) extends Query
  case class Or(q1: Query, q2: Query) extends Query
  case class Exact(tag: Tag) extends Query

  def transactionHistory(a: Address) = Or(Exact(Tag.From(a)), Exact(Tag.To(a)))
}
