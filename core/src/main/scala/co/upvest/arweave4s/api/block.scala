package co.upvest.arweave4s.api

import co.upvest.arweave4s.adt.Block
import co.upvest.arweave4s.marshalling.Marshaller
import com.softwaremill.sttp.circe.asJson
import com.softwaremill.sttp.sttp

object block {

  import Marshaller._
  import co.upvest.arweave4s.utils.SttpExtensions.syntax._

  def current[F[_]]()(implicit send: Backend[F], jh: JsonHandler[F]): F[Block] = jh(
    send(sttp.get("current_block" :: Nil) response asJson[Block])
  )

  def get[F[_]](ih: Block.IndepHash)(implicit send: Backend[F], jh: JsonHandler[F]): F[Block] = jh(
    send(sttp.get("block" :: "hash" :: s"$ih" :: Nil) response asJson[Block])
  )

  def get[F[_]](height: BigInt)(implicit send: Backend[F], jh: JsonHandler[F]): F[Block] = jh(
    send(sttp.get("block" :: "height" :: s"$height" :: Nil) response asJson[Block])
  )
}

