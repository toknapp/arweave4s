package co.upvest.arweave4s.utils

import cats.data.NonEmptyList
import cats.syntax.either._
import cats.syntax.flatMap._
import cats.{MonadError, ~>}
import com.softwaremill.sttp.{Request, Response, SttpBackend, Uri}

import scala.util.Random

class MultipleHostsBackend[R[_], S, G[_]](b: SttpBackend[G, S], uris: NonEmptyList[Uri],
                                          permute: NonEmptyList[Uri] => NonEmptyList[Uri])(
                                           implicit R: MonadError[R, NonEmptyList[Throwable]],
                                           i : G ~> R) extends SttpBackend[R, S] {
  import SttpExtensions._

  private val G = b.responseMonad

  // NB. send just delegates to the provided backend
  def send[T](req: Request[T, S]): R[Response[T]] = i(b.send(req))

  def apply[T](req: PartialRequest[T, S]): R[Response[T]] = {
    def f(u: Uri): R[Either[Throwable, Response[T]]] = i(
      G.handleError (
        G.map (b send completeRequest(req, u)) { _.asRight[Throwable] }
      ) { case t  => G unit t.asLeft[Response[T]] }
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

  def close(): Unit = b.close()

  val responseMonad = new com.softwaremill.sttp.MonadError[R] {
    def error[T](t: Throwable): R[T] = R raiseError NonEmptyList.one(t)
    def flatMap[T, T2](fa: R[T])(f: T => R[T2]): R[T2] = R.flatMap(fa)(f)
    protected def handleWrappedError[T](rt: R[T])(h: PartialFunction[Throwable,R[T]]): R[T] =
      R.recoverWith(rt)(PartialFunction.empty)
    def map[T, T2](fa: R[T])(f: T => T2): R[T2] = R.map(fa)(f)
    def unit[T](t: T): R[T] = R pure t
  }
}

object MultipleHostsBackend {

  def apply[R[_], S, G[_]](b: SttpBackend[G, S],
                           uris: NonEmptyList[Uri],
                           permute: NonEmptyList[Uri] => NonEmptyList[Uri])
                          (implicit R: MonadError[R, NonEmptyList[Throwable]],
                           i: G ~> R): MultipleHostsBackend[R, S, G] =
    new MultipleHostsBackend(b, uris, permute)

  val uniform: NonEmptyList[Uri] => NonEmptyList[Uri] = { nl =>
    val l = Random.shuffle(nl.toList)
    NonEmptyList(l.head, l.tail)
  }

  def retry(n:Int): NonEmptyList[Uri] => NonEmptyList[Uri] =
    _ >>= { e =>
      NonEmptyList(e, List.fill(n)(e))
    }
}
