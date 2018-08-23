package co.upvest.arweave4s.api

import co.upvest.arweave4s.adt.Info
import co.upvest.arweave4s.marshalling.Marshaller
import com.softwaremill.sttp.circe.asJson
import com.softwaremill.sttp.sttp

object info  {

  import Marshaller._
  import co.upvest.arweave4s.utils.SttpExtensions.syntax._

  def apply[F[_]]()(implicit send: Backend[F], jh: JsonHandler[F]): F[Info] = jh(
    send(sttp.get("info" :: Nil) response asJson[Info])
  )
}
