package co.upvest.arweave4s.api

import co.upvest.arweave4s.adt.Peer
import co.upvest.arweave4s.marshalling.Marshaller
import com.softwaremill.sttp.circe.asJson
import com.softwaremill.sttp.sttp

object peers {

  import Marshaller._
  import co.upvest.arweave4s.utils.SttpExtensions.syntax._

  def apply[F[_]]()(implicit send: Backend[F], jh: JsonHandler[F]): F[List[Peer]] = jh(
    send(sttp.get("peers" :: Nil) response asJson[List[Peer]])
  )
}
