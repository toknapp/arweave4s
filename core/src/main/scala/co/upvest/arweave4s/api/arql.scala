package co.upvest.arweave4s.api

import co.upvest.arweave4s.adt.{Query, Transaction}
import com.softwaremill.sttp.circe.asJson
import com.softwaremill.sttp.sttp
import com.softwaremill.sttp.circe._

object arql {

  import Marshaller._
  import co.upvest.arweave4s.utils.SttpExtensions.syntax._

  def apply[F[_]](q: Query)(implicit send: Backend[F], jh: JsonHandler[F]): F[Seq[Transaction.Id]] =
    jh(
      send(
        sttp.body(q).post("arql" :: Nil) response asJson[Seq[Transaction.Id]])
    )
}
