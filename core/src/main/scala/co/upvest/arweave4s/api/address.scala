package co.upvest.arweave4s.api

import cats.Functor
import co.upvest.arweave4s.adt.{Address, Transaction, Winston}
import co.upvest.arweave4s.utils.RequestHandling
import com.softwaremill.sttp.{Uri, sttp}
import io.circe.parser._


object address {

  import Marshaller._

  def lastTxM[F[_], G[_]](address: Address)(implicit c: AbstractConfig[F, G],
                                            jh: JsonHandler[F],
                                            FT: Functor[F]) = {
    val resp = RequestHandling
      .process[F,G, Option[Transaction.Id]](
      "wallet" :: s"$address" :: "last_tx" :: Nil,
      { uri: Uri =>
        sttp.get(uri).mapResponse { tx =>
          val tt = if(tx.isEmpty)
            """{"tx_id_injected" : ""}"""
          else
            s"""{"tx_id_injected" : "$tx" }"""
          println(tt)
          decode[Option[Transaction.Id]] (tt)
        }
      }
    )
    resp.map(jh.apply _)
  }

  def lastTx[F[_], G[_]](address: Address)(implicit c: AbstractConfig[F, G],
                                           jh: JsonHandler[F], FT: Functor[F]):
    F[Option[Transaction.Id]] = lastTxM(address).head

  def balanceM[F[_], G[_]](address: Address)(implicit
                                            c: AbstractConfig[F, G],
                                             jh: JsonHandler[F],
                                             FT: Functor[F]
  ): List[F[Winston]] = RequestHandling
    .process[F, G, Winston](
    "wallet" :: s"$address" :: "balance" :: Nil,
    sttp.get(_).mapResponse(w => decode[Winston](s"""{"w":"$w"}""")),
  ).map(jh.apply _)



  def balance[F[_], G[_]](address: Address)(implicit
                                            c: AbstractConfig[F, G],
                                            jh: JsonHandler[F],
                                            FT: Functor[F]): F[Winston]
  = balanceM(address).head
}
