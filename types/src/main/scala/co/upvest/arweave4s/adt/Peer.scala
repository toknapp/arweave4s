package co.upvest.arweave4s.adt

import com.softwaremill.sttp.{Uri, UriContext}

import scala.util.Try

case class Peer(uri: Uri)

object Peer {
  def apply(rawString: String): Try[Peer] = Try { apply(uri"$rawString") }
}
