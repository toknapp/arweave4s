package co.upvest.arweave4s.api

import co.upvest.arweave4s.utils.EmptyStringAsNone
import co.upvest.arweave4s.adt.{Block, Transaction, Address, Winston, Signed}
import com.softwaremill.sttp.circe._
import com.softwaremill.sttp.{Response, SttpBackend, sttp, UriContext}
import io.circe
import v1.marshalling.MarshallerV1

import cats.{MonadError, ~>, Id}
import cats.arrow.FunctionK
import cats.evidence.As
import cats.syntax.flatMap._
import cats.syntax.applicative._
import cats.syntax.applicativeError._

import scala.util.Try

object highlevel extends MarshallerV1 {

  type JsonHandler[F[_]] = λ[α => F[Response[Either[circe.Error, α]]]] ~> F
  type EncodedStringHandler[F[_]] = λ[α => F[Response[Option[α]]]] ~> F
  type SuccessHandler[F[_]] = F[Response[Unit]] => F[Unit]

  trait AbstractConfig[F[_], G[_]] {
    def host: String
    def backend: SttpBackend[G, _]
    def i: G ~> F
  }

  case class Config[F[_]](
    host: String, backend: SttpBackend[F, _]
  ) extends AbstractConfig[F, F] {
    override val i = FunctionK.id
  }

  case class FullConfig[F[_], G[_]](
    host: String, backend: SttpBackend[G, _], i: G ~> F
  ) extends AbstractConfig[F, G]

  sealed trait Failure extends Exception
  case class HttpFailure(rsp: Response[_]) extends Failure
  case class DecodingFailure(t: Exception) extends Failure
  case object InvalidEncoding extends Failure // TODO: more informative

  trait MonadErrorInstances {
    implicit def monadErrorJsonHandler[F[_]: MonadError[?[_], T], T](
      implicit as: Failure As T
    ): JsonHandler[F] =
      λ[λ[α => F[Response[Either[circe.Error, α]]]] ~> F]{
        _ >>= { rsp =>
          rsp.body match {
            case Left(_) => as.coerce(HttpFailure(rsp)).raiseError
            case Right(Left(e)) => as.coerce(DecodingFailure(e)).raiseError
            case Right(Right(a)) => a.pure[F]
          }
        }
      }

    implicit def monadErrorEncodedStringHandler[F[_]: MonadError[?[_], T], T](
      implicit as: Failure As T
    ): EncodedStringHandler[F] = λ[λ[α => F[Response[Option[α]]]] ~> F]{
      _ >>= { rsp =>
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
      λ[λ[α => Id[Response[Either[circe.Error, α]]]] ~> Id]{ rsp =>
        rsp.body match {
          case Left(_) => throw HttpFailure(rsp)
          case Right(Left(e)) => throw DecodingFailure(e)
          case Right(Right(a)) => a
        }
      }

    implicit def idEncodedStringHandler: EncodedStringHandler[Id] =
      λ[λ[α => Id[Response[Option[α]]]] ~> Id]{ rsp =>
        rsp.body match {
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

  object block {
    def current[F[_], G[_]]()(implicit
      c: AbstractConfig[F, G], jh: JsonHandler[F]
    ): F[Block] = {
      val req = sttp.get(uri"${c.host}/current_block").response(asJson[Block])
      jh(c.i(c.backend send req))
    }

    def get[F[_], G[_]](ih: Block.IndepHash)(implicit
      c: AbstractConfig[F, G], jh: JsonHandler[F]
    ): F[Block] = {
      val req = sttp.get(uri"${c.host}/block/hash/$ih"
      ).response(asJson[Block])
      jh(c.i(c.backend send req))
    }

    def get[F[_], G[_]](height: BigInt)(implicit
      c: AbstractConfig[F, G], jh: JsonHandler[F]
    ): F[Block] = {
      val req = sttp.get(
        uri"${c.host}/block/height/$height"
      ).response(asJson[Block])
      jh(c.i(c.backend send req))
    }
  }

  object address {
    def lastTx[F[_], G[_]](address: Address)(implicit
      c: AbstractConfig[F, G], esh: EncodedStringHandler[F]
    ): F[Option[Transaction.Id]] = {
      val req = sttp
        .get(uri"${c.host}/wallet/$address/last_tx")
        .mapResponse { s =>
          EmptyStringAsNone.of(s).toOption match {
            case None => Some(None)
            case Some(s) => Transaction.Id.fromEncoded(s) map Some.apply
          }
        }
      esh(c.i(c.backend send req))
    }

    def balance[F[_], G[_]](address: Address)(implicit
      c: AbstractConfig[F, G], esh: EncodedStringHandler[F]
    ): F[Winston] = {
      val req = sttp
        .get(uri"${c.host}/wallet/$address/balance")
        .mapResponse(winstonMapper)
      esh(c.i(c.backend send req))
    }
  }

  object tx {
    def get[F[_], G[_]](txId: Transaction.Id)(implicit
      c: AbstractConfig[F, G], jh: JsonHandler[F]
    ): F[Transaction] = {
      val req = sttp
        .get(uri"${c.host}/tx/$txId")
        .response(asJson[Transaction])
      jh(c.i(c.backend send req))
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
  }

  object price {
    def estimateForBytes[F[_], G[_]](bytes: BigInt)(implicit
      c: AbstractConfig[F, G], esh: EncodedStringHandler[F]
    ): F[Winston] = {
      val req = sttp
        .get(uri"${c.host}/price/$bytes")
        .mapResponse(winstonMapper)
      esh(c.i(c.backend send req))
    }
  }

  private def winstonMapper(s: String) = Try { Winston.apply(s) } toOption
}
