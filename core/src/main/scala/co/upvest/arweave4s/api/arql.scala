package co.upvest.arweave4s.api

import co.upvest.arweave4s.adt.{Query, Transaction}
import co.upvest.arweave4s.marshalling.Marshaller
import com.softwaremill.sttp.circe.{asJson, _}
import com.softwaremill.sttp.sttp

object arql {

  import Marshaller._
  import co.upvest.arweave4s.utils.SttpExtensions.syntax._

  def apply[F[_]](q: Query)(implicit send: Backend[F], jh: JsonHandler[F]): F[Seq[Transaction.Id]] =
    jh(
      send(
        sttp.body(q).post("arql" :: Nil) response asJson[Seq[Transaction.Id]]
      )
    )
}
