package co.upvest.arweave4s

import cats.evidence.As
import cats.data.NonEmptyList
import cats.syntax.applicative._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import cats.{Id, MonadError, ~>}
import co.upvest.arweave4s.utils.SttpExtensions.{PartialRequest, completeRequest}
import co.upvest.arweave4s.utils.MultipleHostsBackend
import com.softwaremill.sttp.{Response, SttpBackend, Uri}
import io.circe

import scala.concurrent.{ExecutionContext, Future}

package object api {

  type JsonHandler[F[_]] = λ[α => F[Response[Either[circe.Error, α]]]] ~> F
  type EncodedStringHandler[F[_]] = λ[α => F[Response[Option[α]]]] ~> F
  type SuccessHandler[F[_]] = F[Response[Unit]] => F[Unit]

  trait Backend[F[_]] {
    def apply[T](r: PartialRequest[T, Nothing]): F[Response[T]]

  }

  object Backend {
    implicit def fromMHB[R[_], G[_]](mhb: MultipleHostsBackend[R, G]): Backend[R] = new Backend[R] {
      def apply[T](r: PartialRequest[T, Nothing]): R[Response[T]] = mhb(r)
    }

    def lift[F[_], G[_]](backend: Backend[G], i: G ~> F) = new Backend[F] {
      override def apply[T](r: PartialRequest[T, Nothing]): F[Response[T]] = i(backend(r))
    }
  }

  case class Config[F[_]](host: Uri, backend: SttpBackend[F, Nothing]) extends Backend[F] {
    override def apply[T](r: PartialRequest[T, Nothing]): F[Response[T]] = {
      backend send completeRequest[T, Nothing](r, host)
    }
  }

  sealed abstract class Failure(message: String, cause: Option[Throwable])
    extends Exception(message, cause.orNull)

  object Failure {
    implicit def injectMultipleFailures(nel: NonEmptyList[Throwable]): Failure =
      MultipleUnderlyingFailures(nel)
  }

  case class HttpFailure(rsp: Response[_])
    extends Failure(s"HTTP failure: $rsp", None)
  case class DecodingFailure(t: Exception)
    extends Failure("Decoding failure", Some(t))
  case object InvalidEncoding
    extends Failure("invalid encoding", None) // TODO: more informative
  case class MultipleUnderlyingFailures(nel: NonEmptyList[Throwable])
    extends Failure("Multiple underlying failures", Some(nel.head))

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


}
