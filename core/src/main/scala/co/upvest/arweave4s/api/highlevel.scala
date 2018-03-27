package co.upvest.arweave4s.api

import co.upvest.arweave4s.adt.Block
import com.softwaremill.sttp.circe._
import com.softwaremill.sttp.{Response, SttpBackend, sttp, UriContext}
import io.circe
import v1.marshalling.MarshallerV1

import cats.{MonadError, ~>, Id}
import cats.evidence.As
import cats.syntax.flatMap._
import cats.syntax.applicative._
import cats.syntax.applicativeError._

object highlevel extends MarshallerV1 {

  type JsonHandler[F[_]] = λ[α => F[Response[Either[circe.Error, α]]]] ~> F

  case class Config[F[_]](host: String, backend: SttpBackend[F, _])

  sealed trait Failure extends Exception
  case class HttpFailure(rsp: Response[_]) extends Failure
  case class DecodingFailure(t: Exception) extends Failure

  trait MonadErrorInstances {
    implicit def monadErrorJsonHandler[F[_]: MonadError[?[_], T], T](
      implicit as: Failure As T
    ): JsonHandler[F] =
      λ[λ[α => F[Response[Either[circe.Error, α]]]] ~> F]{
        _ >>= { rsp =>
          rsp.body match {
            case Left(_) => as.coerce(HttpFailure(rsp)).raiseError
            case Right(Left(e)) => as.coerce(DecodingFailure(e)).raiseError
            case Right(Right(a)) => a.pure
          }
        }
      }
  }

  object monadError extends MonadErrorInstances

  trait IdInstances {
    implicit def idJsonHandler: JsonHandler[Id] =
      λ[λ[α => Id[Response[Either[circe.Error, α]]]] ~> Id]{ rsp =>
        rsp.body match {
          case Left(_) => throw HttpFailure(rsp)
          case Right(Left(e)) => throw DecodingFailure(e)
          case Right(Right(a)) => a
        }
      }
  }

  object id extends IdInstances

  object block {
    private val blockPath = "block"

    def current[F[_]]()(implicit
      c: Config[F],
      jh: JsonHandler[F]
    ): F[Block] = {
      val req = sttp.get(uri"${c.host}/current_block").response(asJson[Block])
      jh(c.backend.send(req))
    }

    def get[F[_]](ih: Block.IndepHash)(implicit
      c: Config[F],
      jh: JsonHandler[F]
    ): F[Block] = {
      val req = sttp.get(uri"${c.host}/$blockPath/hash/$ih"
      ).response(asJson[Block])
      jh(c.backend.send(req))
    }

    def get[F[_]](height: BigInt)(implicit
      c: Config[F],
      jh: JsonHandler[F]
    ): F[Block] = {
      val req = sttp.get(
        uri"${c.host}/$blockPath/height/$height"
      ).response(asJson[Block])
      jh(c.backend.send(req))
    }
  }
}
