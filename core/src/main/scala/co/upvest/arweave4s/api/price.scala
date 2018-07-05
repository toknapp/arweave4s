package co.upvest.arweave4s.api

import co.upvest.arweave4s.adt.{Address, Data, Winston}
import com.softwaremill.sttp.sttp

object price {

  import Marshaller._
  import co.upvest.arweave4s.utils.SttpExtensions.syntax._

  def transferTransactionTo[F[_]](recipient: Address)(implicit send: Backend[F], esh: EncodedStringHandler[F]):
    F[Winston] = esh(
      send(sttp.get("price" :: "0" :: recipient.toString :: Nil)
        .mapResponse(winstonMapper)
      )
  )

  def dataTransaction[F[_]](d: Data)(implicit send: Backend[F], esh: EncodedStringHandler[F]):
    F[Winston] = esh(
      send(sttp.get("price" :: s"${d.bytes.length}" :: Nil)
        .mapResponse(winstonMapper)
      )
  )
}
