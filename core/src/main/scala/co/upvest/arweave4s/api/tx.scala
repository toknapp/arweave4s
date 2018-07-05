package co.upvest.arweave4s.api

import cats.Monad
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import co.upvest.arweave4s.adt._
import com.softwaremill.sttp.circe._
import com.softwaremill.sttp.{asString, sttp}
import io.circe.parser.decode

object tx {

  import Marshaller._
  import co.upvest.arweave4s.utils.SttpExtensions.syntax._

  def get[F[_] : Monad](txId: Transaction.Id)(implicit send: Backend[F], jh: JsonHandler[F]):
    F[Transaction.WithStatus] = send(sttp.get("tx" :: s"$txId" :: Nil)
      .response(asString)) >>= { rsp =>
        (rsp.code, rsp.body) match {
          case (404, _) => Transaction.WithStatus.NotFound(txId).pure widen
          case (410, _) => Transaction.WithStatus.Gone(txId).pure widen
          case (202, _) => Transaction.WithStatus.Pending(txId).pure widen
          case (_, Right(str)) =>
            jh(
              rsp.copy(rawErrorBody = rsp.body.map(decode[Signed[Transaction]])
                .left
                .map(_.getBytes("UTF-8")))
                .pure
            ) map Transaction.WithStatus.Accepted
          case (_, Left(l)) => jh(rsp.copy(rawErrorBody = Left(l getBytes "UTF-8")).pure)
        }
  }

  def submit[F[_]](tx: Signed[Transaction])(implicit send: Backend[F], sh: SuccessHandler[F]):
    F[Unit] = sh(
      send(sttp.body(tx).post("tx" :: Nil)
        .mapResponse(_ => ())
      )
  )

  def pending[F[_] : Monad]()(implicit send: Backend[F], jh: JsonHandler[F]):
    F[Seq[Transaction.Id]] =
      jh(
        send(sttp.get("tx" :: "pending" :: Nil)
          .response(asJson))
      )
}

