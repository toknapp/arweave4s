package co.upvest.arweave4s

import cats.arrow.FunctionK
import cats.evidence.As
import cats.syntax.applicative._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.{Id, Monad, MonadError, ~>}
import co.upvest.arweave4s.adt._
import com.softwaremill.sttp.circe._
import com.softwaremill.sttp.{Response, SttpBackend, UriContext, asString, sttp}
import io.circe
import io.circe.parser.decode

import scala.concurrent.{ExecutionContext, Future}
import scala.language.{higherKinds, postfixOps}

package object api {

  import Marshaller._

  type JsonHandler[F[_]] = λ[α => F[Response[Either[circe.Error, α]]]] ~> F
  type EncodedStringHandler[F[_]] = λ[α => F[Response[Option[α]]]] ~> F
  type SuccessHandler[F[_]] = F[Response[Unit]] => F[Unit]

  trait AbstractConfig[F[_], G[_]] {
    def host: String
    def backend: SttpBackend[G, _]
    def i: G ~> F
  }

  case class Config[F[_]](
                           host: String, backend: SttpBackend[F, _], retries: Int
  ) extends AbstractConfig[F, F] {
    override val i = FunctionK.id
  }

  case class FullConfig[F[_], G[_]](
                                     host: String, backend: SttpBackend[G, _], i: G ~> F, retries: Int
  ) extends AbstractConfig[F, G]

  sealed abstract class Failure(message: String, cause: Option[Throwable])
    extends Exception(message, cause.orNull)
  case class HttpFailure(rsp: Response[_])
    extends Failure(s"HTTP failure: $rsp", None)
  case class DecodingFailure(t: Exception)
    extends Failure("Decoding failure", Some(t))
  case object InvalidEncoding
    extends Failure("invalid encoding", None) // TODO: more informative




  trait MonadErrorInstances {
    implicit def monadErrorJsonHandler[F[_]: MonadError[?[_], T], T](
      implicit as: Failure As T
    ): JsonHandler[F] = new (λ[α => F[Response[Either[circe.Error, α]]]] ~> F) {
      override def apply[A](fa: F[Response[Either[circe.Error, A]]]): F[A] =
        fa >>= { rsp =>
          rsp.body match {
            case Left(_) => as.coerce(HttpFailure(rsp)).raiseError
            case Right(Left(e)) => as.coerce(DecodingFailure(e)).raiseError
            case Right(Right(a)) => a.pure[F]
          }
        }
    }

    implicit def monadErrorEncodedStringHandler[F[_]: MonadError[?[_], T], T](
      implicit as: Failure As T
    ): EncodedStringHandler[F] = new (λ[α => F[Response[Option[α]]]] ~> F){
      override def apply[A](fa: F[Response[Option[A]]]): F[A] =
        fa >>= { rsp =>
          rsp.body match {
          case Left(_) => as.coerce(HttpFailure(rsp)).raiseError
          case Right(None) => as.coerce(InvalidEncoding).raiseError
          case Right(Some(a)) => a.pure[F]
        }
      }
    }

    implicit def monadErrorSuccessHandler[F[_]: MonadError[?[_], T], T](
      implicit as: Failure As T
    ): SuccessHandler[F] = {
      _ >>= { rsp =>
        rsp.body match {
          case Left(_) => as.coerce(HttpFailure(rsp)).raiseError
          case Right(_) => ().pure[F]
        }
      }
    }
  }

  object monadError extends MonadErrorInstances

  trait IdInstances {
    implicit def idJsonHandler: JsonHandler[Id] =
      new (λ[α => Id[Response[Either[circe.Error, α]]]] ~> Id) {
        override def apply[A](rsp: Id[Response[Either[circe.Error, A]]]): A = rsp.body match {
          case Left(_) => throw HttpFailure(rsp)
          case Right(Left(e)) => throw DecodingFailure(e)
          case Right(Right(a)) => a
        }
      }

    implicit def idEncodedStringHandler: EncodedStringHandler[Id] = new (λ[α => Id[Response[Option[α]]]] ~> Id) {
      override def apply[A](rsp: Id[Response[Option[A]]]): A = rsp.body match {
        case Left(_) => throw HttpFailure(rsp)
        case Right(None) => throw InvalidEncoding
        case Right(Some(a)) => a
      }
    }


    implicit def idSuccessHandler: SuccessHandler[Id] = { rsp =>
      rsp.body.right getOrElse { throw HttpFailure(rsp) }
    }
  }


  object id extends IdInstances

  trait FutureInstances {
    implicit def futureJsonHandler(implicit ec:ExecutionContext): JsonHandler[Future] =
      new (λ[α => Future[Response[Either[circe.Error, α]]]] ~> Future){
        override def apply[A](frsp: Future[Response[Either[circe.Error, A]]])=
          frsp map { rsp =>
            rsp.body match {
              case Left(_) => throw HttpFailure(rsp)
              case Right(Left(e)) => throw DecodingFailure(e)
              case Right(Right(a)) => a
          }}
      }

    implicit def futureJsonHandlerEncodedStringHandler(implicit ec:ExecutionContext): EncodedStringHandler[Future] =
      new (λ[α => Future[Response[Option[α]]]] ~> Future) {
        override def apply[A](frsp: Future[Response[Option[A]]]) =
          frsp map { rsp =>
            rsp.body match {
              case Left(_) => throw HttpFailure(rsp)
              case Right(None) => throw InvalidEncoding
              case Right(Some(a)) => a
            }
        }
    }

    implicit def futureJsonHandlerSuccessHandler(implicit ec:ExecutionContext): SuccessHandler[Future] = { frsp =>
      frsp.flatMap { rsp =>
        rsp.body
          .map(Future.successful)
          .getOrElse(Future.failed(HttpFailure(rsp)))
      }
    }
  }

  object future extends FutureInstances

  object tx {
    def get[F[_]: Monad, G[_]](txId: Transaction.Id)(implicit
      c: AbstractConfig[F, G], jh: JsonHandler[F]
    ): F[Transaction.WithStatus] = {
      val req = sttp.get(uri"${c.host}/tx/$txId").response(asString)

      c.i(c.backend send req) >>= { rsp =>
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
    }
    def submit[F[_], G[_]](tx: Signed[Transaction])(implicit
      c: AbstractConfig[F, G], sh: SuccessHandler[F]
    ): F[Unit] = {
      val req = sttp
        .body(tx)
        .post(uri"${c.host}/tx")
        .mapResponse { _ => () }
      sh(c.i(c.backend send req))
    }

    def pending[F[_]: Monad, G[_]]()(implicit
      c: AbstractConfig[F, G], jh: JsonHandler[F]
    ): F[Seq[Transaction.Id]] =
      jh(c.i(
        c.backend send sttp.get(uri"${c.host}/tx/pending").response(asJson)
      ))
  }

  object price {

    def transferTransactionTo[F[_], G[_]](recipient: Address)(implicit
      c: AbstractConfig[F, G], esh: EncodedStringHandler[F]
    ): F[Winston] = {
      val req = sttp.get(uri"${c.host}/price/0/$recipient")
        .mapResponse(winstonMapper)
      esh(c.i(c.backend send req))
    }

    def dataTransaction[F[_], G[_]](d: Data)(implicit
      c: AbstractConfig[F, G], esh: EncodedStringHandler[F]
    ): F[Winston] = {
      val req = sttp.get(uri"${c.host}/price/${d.bytes.length}")
        .mapResponse(winstonMapper)
      esh(c.i(c.backend send req))
    }
  }

  object arql {
    def apply[F[_], G[_]](q: Query)(implicit
      c: AbstractConfig[F, G], jh: JsonHandler[F]
    ): F[Seq[Transaction.Id]] = {
      val req = sttp
        .body(q)
        .post(uri"${c.host}/arql")
        .response(asJson[Seq[Transaction.Id]])
      jh(c.i(c.backend send req))
    }
  }

  object info {
    def apply[F[_], G[_]]()(implicit
      c: AbstractConfig[F, G], jh: JsonHandler[F]
    ): F[Info] = {
      val req = sttp.get(uri"${c.host}/info")
        .response(asJson[Info])
      jh(c.i(c.backend send req))
    }
  }

}
