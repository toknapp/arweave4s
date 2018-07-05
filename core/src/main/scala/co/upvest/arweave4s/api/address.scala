package co.upvest.arweave4s.api

import co.upvest.arweave4s.adt.{Address, Transaction, Winston}
import co.upvest.arweave4s.utils.EmptyStringAsNone
import com.softwaremill.sttp.{UriContext, sttp}

object address {
  import Marshaller.winstonMapper

  def lastTx[F[_], G[_]](address: Address)(implicit
                                           c: AbstractConfig[F, G], esh: EncodedStringHandler[F]
  ): F[Option[Transaction.Id]] = {
    val req = sttp
      .get(uri"${c.host}/wallet/$address/last_tx")
      .mapResponse { s =>
        EmptyStringAsNone.of(s).toOption match {
          case None => Some(None)
          case Some(s) => Transaction.Id.fromEncoded(s) map Some.apply
        }
      }
    esh(c.i(c.backend send req))
  }

  def balance[F[_], G[_]](address: Address)(implicit
                                            c: AbstractConfig[F, G], esh: EncodedStringHandler[F]
  ): F[Winston] = {
    val req = sttp
      .get(uri"${c.host}/wallet/$address/balance")
      .mapResponse(winstonMapper)
    esh(c.i(c.backend send req))
  }
}
