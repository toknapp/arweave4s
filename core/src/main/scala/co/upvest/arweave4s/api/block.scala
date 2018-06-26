package co.upvest.arweave4s.api

import cats.Functor
import co.upvest.arweave4s.adt.Block
import co.upvest.arweave4s.utils.RequestHandling
import com.softwaremill.sttp.sttp

object block {

  import Marshaller._

  def currentM[F[_], G[_]]()(implicit c: AbstractConfig[F, G],
                             jh: JsonHandler[F],
                             FT: Functor[F]): List[F[Block]] = RequestHandling
    .process[F,G,Block](
      "current_block" :: Nil,
      sttp.get
    ).map(jh.apply _)

  def current[F[_], G[_]]()(implicit c: AbstractConfig[F, G],
                            jh: JsonHandler[F],
                            FT: Functor[F]): F[Block] = currentM.head

  def getM[F[_], G[_]](ih: Block.IndepHash)
                      (implicit c: AbstractConfig[F, G],
                       jh: JsonHandler[F],
                       FT: Functor[F]
                      ): List[F[Block]] = RequestHandling.
    process[F, G, Block](
      "block" :: "hash" :: ih.toString :: Nil,
      sttp.get
    ).map(jh.apply _)

  def get[F[_], G[_]](ih: Block.IndepHash)
                     (implicit c: AbstractConfig[F, G],
                      jh: JsonHandler[F],
                      FT: Functor[F]): F[Block] = getM(ih).head



  def getM[F[_], G[_]](height: BigInt)
                      (implicit c: AbstractConfig[F, G],
                       jh: JsonHandler[F],
                       FT: Functor[F]): List[F[Block]] = RequestHandling.
    process[F, G, Block](
      "block" :: "height" :: height.toString :: Nil,
      sttp.get
    ).map(jh.apply _)

  def get[F[_], G[_]](height: BigInt)
                     (implicit c: AbstractConfig[F, G],
                      jh: JsonHandler[F],
                      FT: Functor[F]): F[Block] = getM(height).head
}
