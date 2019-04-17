package co.upvest.arweave4s.api

import co.upvest.arweave4s.adt.{Block, WalletResponse, Address}
import co.upvest.arweave4s.marshalling.Marshaller
import com.softwaremill.sttp.circe.asJson
import com.softwaremill.sttp.sttp

import cats.Functor
import cats.syntax.functor._

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

  def wallets[F[_]: Functor](ih: Block.IndepHash)(implicit send: Backend[F], jh: JsonHandler[F]): F[Map[Address, WalletResponse]] = jh(
    send(sttp.get("block" :: "hash" :: s"$ih" :: "wallet_list" :: Nil) response asJson[List[WalletResponse]])
  ) map { _ map { w => (w.address, w) } toMap }

  def hashList[F[_]](ih: Block.IndepHash)(implicit send: Backend[F], jh: JsonHandler[F]): F[List[Block.IndepHash]] = jh(
    send(sttp.get("block" :: "hash" :: s"$ih" :: "hash_list" :: Nil) response asJson)
  )
}

