package co.upvest.arweave4s.adt

case class Peer(host: String)

object Peer {
  def apply(rawString: String): Peer = new Peer(rawString)
}
