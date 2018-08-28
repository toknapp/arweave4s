package co.upvest.arweave4s.api

import co.upvest.arweave4s.adt.{Address, Transaction, Winston}
import co.upvest.arweave4s.marshalling.Marshaller
import com.softwaremill.sttp.sttp

object address {

  import Marshaller._
  import co.upvest.arweave4s.utils.SttpExtensions.syntax._

  def lastTx[F[_]](address: Address)(implicit send: Backend[F], esh: EncodedStringHandler[F]): F[Option[Transaction.Id]] =
    esh(
      send(
        sttp.get("wallet" :: s"$address" :: "last_tx" :: Nil)
          .mapResponse(mapEmptyString)
      )
    )

  def balance[F[_]](address: Address)(implicit send: Backend[F], esh: EncodedStringHandler[F]): F[Winston] =
    esh(
      send(
        sttp.get("wallet" :: s"$address" :: "balance" :: Nil)
          .mapResponse(winstonMapper)
      )
    )
}
