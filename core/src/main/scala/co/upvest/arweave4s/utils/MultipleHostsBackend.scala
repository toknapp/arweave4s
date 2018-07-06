package co.upvest.arweave4s.utils

import cats.arrow.FunctionK
import cats.data.NonEmptyList
import cats.syntax.either._
import cats.syntax.flatMap._
import cats.{~>, Monad}
import com.softwaremill.sttp.{Response, SttpBackend, Uri}

import scala.util.Random

class MultipleHostsBackend[R[_], G[_]](
  b: SttpBackend[G, Nothing],
  uris: NonEmptyList[Uri],
  permute: NonEmptyList[Uri] => NonEmptyList[Uri])(
    implicit R: Monad[R],
    raiseError: RaiseError[R, NonEmptyList[Throwable]],
    i: G ~> R) {

  import SttpExtensions._

  private val G = b.responseMonad

  def apply[T](req: PartialRequest[T, Nothing]): R[Response[T]] = {
    def f(u: Uri): R[Either[Throwable, Response[T]]] = i(
      G.handleError (
        G.map (b send completeRequest(req, u)) { _.asRight[Throwable] }
      ) { case t  => G unit t.asLeft[Response[T]] }
    )

    def go(ts: NonEmptyList[Throwable]): List[Uri] => R[Response[T]] = {
      case Nil => raiseError(ts)
      case u :: us =>
        f(u) >>= {
          case Right(rsp) => R pure rsp
          case Left(t) => go(t :: ts)(us)
        }
    }

    permute(uris) match {
      case NonEmptyList(u, us) =>
        f(u) >>= {
          case Right(rsp) => R pure rsp
          case Left(t) => go(NonEmptyList.one(t))(us)
        }
    }
  }
}

object MultipleHostsBackend {

  def apply[R[_]: Monad : RaiseError[?[_], NonEmptyList[Throwable]], G[_]](
    b: SttpBackend[G, Nothing],
    uris: NonEmptyList[Uri],
    permute: NonEmptyList[Uri] => NonEmptyList[Uri]
  )(implicit i: G ~> R) = new MultipleHostsBackend(b, uris, permute)

  def apply[F[_]: Monad : RaiseError[?[_], NonEmptyList[Throwable]]](
    b: SttpBackend[F, Nothing],
    uris: NonEmptyList[Uri],
    permute: NonEmptyList[Uri] => NonEmptyList[Uri]
  ) = {
    implicit val i = FunctionK.id[F]
    new MultipleHostsBackend(b, uris, permute)
  }

  val uniform: NonEmptyList[Uri] => NonEmptyList[Uri] = { nl =>
    val l = Random.shuffle(nl.toList)
    NonEmptyList(l.head, l.tail)
  }

  def retry(n:Int): NonEmptyList[Uri] => NonEmptyList[Uri] =
    _ >>= { e =>
      NonEmptyList(e, List.fill(n)(e))
    }
}
