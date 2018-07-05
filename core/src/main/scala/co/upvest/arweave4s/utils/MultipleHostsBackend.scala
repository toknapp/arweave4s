package co.upvest.arweave4s.utils

import cats.data.NonEmptyList
import cats.{Id, MonadError, ~>}
import cats.syntax.either._
import cats.syntax.flatMap._

import com.softwaremill.sttp.{Request, Response, SttpBackend, Uri}

import scala.util.Random

class MultipleHostsBackend[R[_], S, G[_]](b: SttpBackend[G, S], uris: NonEmptyList[Uri],
                                          permute: NonEmptyList[Uri] => NonEmptyList[Uri])(
                                           implicit R: MonadError[R, NonEmptyList[Throwable]], i : G ~> R) {

  private val G = b.responseMonad

  private def combineUris(uri0: Uri, uri1: Uri): Uri =
    uri0.copy(path = uri0.path ++ uri1.path)

  private def applyUri[T](req: Request[T, S], u: Uri): Request[T, S] =
    req.copy[Id, T, S](uri = combineUris(req.uri, u))

  def send[T](req: Request[T, S]): R[Response[T]] = {
    def f(u: Uri): R[Either[Throwable, Response[T]]] = i(
      G handleError (
        G.map (b send applyUri(req, u)) _.asRight
        ) { case t  => G unit t asLeft }
    )

    def go(ts: NonEmptyList[Throwable]): List[Uri] => R[Response[T]] = {
      case Nil => R raiseError ts
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

  val uniform: NonEmptyList[Uri] => NonEmptyList[Uri] = { nl =>
    val l = Random.shuffle(nl.toList)
    NonEmptyList(l.head, l.tail)
  }

  def retry(n:Int): NonEmptyList[Uri] => NonEmptyList[Uri] =
    _ >>= { e =>
      NonEmptyList(e, List.fill(n)(e))
    }
}
