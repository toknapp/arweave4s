package co.upvest.arweave4s.utils

import cats.data.NonEmptyList
import cats.syntax.either._
import cats.syntax.flatMap._
import cats.{~>, Monad}
import com.softwaremill.sttp.{Response, SttpBackend, Uri}

import scala.util.Random

class MultipleHostsBackend[R[_], S, G[_]](b: SttpBackend[G, S], uris: NonEmptyList[Uri],
                                          permute: NonEmptyList[Uri] => NonEmptyList[Uri])(
                                           implicit
                                           R: Monad[R],
                                           raiseError: MultipleHostsBackend.RaiseError[R, NonEmptyList[Throwable]],
                                           i: G ~> R
                                           ) {
  import SttpExtensions._

  private val G = b.responseMonad

  def apply[T](req: PartialRequest[T, S]): R[Response[T]] = {
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

  trait RaiseError[F[_], E] {
    def apply[A](e: E): F[A]
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
