package co.upvest.arweave4s.api

import co.upvest.arweave4s.adt.Block
import com.softwaremill.sttp.circe.asJson
import com.softwaremill.sttp.{UriContext, sttp}

object block {
  def current[F[_], G[_]]()(implicit
                            c: AbstractConfig[F, G], jh: JsonHandler[F]
  ): F[Block] = {
    val req = sttp
      .get(uri"${c.host}/current_block")
      .response(asJson[Block])
    jh(c.i(c.backend send req))
  }

  def get[F[_], G[_]](ih: Block.IndepHash)(implicit
                                           c: AbstractConfig[F, G], jh: JsonHandler[F]
  ): F[Block] = {
    val req = sttp
      .get(uri"${c.host}/block/hash/$ih")
      .response(asJson[Block])
    jh(c.i(c.backend send req))
  }

  def get[F[_], G[_]](height: BigInt)(implicit
                                      c: AbstractConfig[F, G], jh: JsonHandler[F]
  ): F[Block] = {
    val req = sttp
      .get(uri"${c.host}/block/height/$height")
      .response(asJson[Block])
    jh(c.i(c.backend send req))
  }
}

