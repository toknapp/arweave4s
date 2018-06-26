package co.upvest.arweave4s.api

import co.upvest.arweave4s.adt.{Address, Transaction, Winston}
import co.upvest.arweave4s.utils.{EmptyStringAsNone, RequestHandling}
import com.softwaremill.sttp.sttp

object address {

  import Marshaller._

  private val mapEmptyTxId = (s: String) => EmptyStringAsNone.of(s).toOption match {
    case None => Some(None)
    case Some(s) => Transaction.Id.fromEncoded(s) map Some.apply
  }


  def lastTxM[F[_], G[_]](address: Address)
                        (implicit c: AbstractConfig[F, G],
                         esh: EncodedStringHandler[F]) = RequestHandling
    .process[F,G, Option[Transaction.Id]](
      "wallet" :: s"$address" :: "last_tx" :: Nil,
      sttp.get (_) .mapResponse(mapEmptyTxId)
    )

  def lastTx[F[_], G[_]](address: Address)(implicit c: AbstractConfig[F, G], esh: EncodedStringHandler[F]
  ): F[Option[Transaction.Id]] = {
    val req = sttp
      .get(uri"${c.hosts}/wallet/$address/last_tx")
      .mapResponse (mapEmptyTxId)
    esh(c.i(c.backend send req))
  }

  def balance[F[_], G[_]](address: Address)(implicit
                                            c: AbstractConfig[F, G], esh: EncodedStringHandler[F]
  ): F[Winston] = {
    val req = sttp
      .get(uri"${c.hosts}/wallet/$address/balance")
      .mapResponse(winstonMapper)
    esh(c.i(c.backend send req))
  }
}
