package co.upvest.arweave4s.api

import co.upvest.arweave4s.adt.Block
import com.softwaremill.sttp.circe._
import com.softwaremill.sttp.{Response, Request, SttpBackend, Uri, sttp, UriContext}
import io.circe
import v1.marshalling.MarshallerV1

import cats.{MonadError, ~>}
import cats.syntax.flatMap._
import cats.syntax.applicative._
import cats.syntax.applicativeError._

object highlevel extends MarshallerV1 {

  type ResponseHandler[F[_]] = λ[α => F[Response[Either[circe.Error, α]]]] ~> F

  trait Config[F[_], S] {
    def host: String
    def backend: SttpBackend[F, S]
    def r: ResponseHandler[F]
  }

  object eitherT {
    sealed trait Failure
    case class HttpFailure(rsp: Response[_]) extends Failure
    case class DecodingFailure(t: Throwable) extends Failure

    case class Config[F[_]: MonadError[?[_], Failure], S](
      host: String,
      backend: SttpBackend[F, S]
    ) extends highlevel.Config[F, S] {
      val r = λ[λ[α => F[Response[Either[circe.Error, α]]]] ~> F]{
        _ >>= { rsp =>
          rsp.body match {
            case Left(_) => (HttpFailure(rsp): Failure).raiseError
            case Right(Left(e)) => (DecodingFailure(e): Failure).raiseError
            case Right(Right(a)) => a.pure
          }
        }
      }
    }
  }

  def currentBlock[F[_], S](implicit c: Config[F, S]): F[Block] =
      go(sttp.get(uris.currentBlock).response(asJson[Block]))

   private def go[F[_], S, A](req: Request[Either[circe.Error, A], S])(
       implicit c: Config[F, S]
   ): F[A] = c.r(c.backend.send(req))

  private object uris {
    def currentBlock[F[_], S](implicit c: Config[F, S]): Uri =
      uri"${c.host}/current_block"
  }
}
