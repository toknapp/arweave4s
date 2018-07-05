package co.upvest.arweave4s.utils

import cats.data.NonEmptyList
import cats.{Id, MonadError, ~>}
import cats.syntax.either._
import cats.syntax.flatMap._

import com.softwaremill.sttp.{RequestT, Request, Response, SttpBackend, Uri, Empty}
import com.softwaremill.sttp.{Method => SMethod}

import scala.util.Random
import scala.collection.immutable.Seq

class MultipleHostsBackend[R[_], S, G[_]](b: SttpBackend[G, S], uris: NonEmptyList[Uri],
                                          permute: NonEmptyList[Uri] => NonEmptyList[Uri])(
                                           implicit R: MonadError[R, NonEmptyList[Throwable]], i : G ~> R) extends SttpBackend[R, S] {
  import MultipleHostsBackend._

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

  val uniform: NonEmptyList[Uri] => NonEmptyList[Uri] = { nl =>
    val l = Random.shuffle(nl.toList)
    NonEmptyList(l.head, l.tail)
  }

  def retry(n:Int): NonEmptyList[Uri] => NonEmptyList[Uri] =
    _ >>= { e =>
      NonEmptyList(e, List.fill(n)(e))
    }

  type PartialRequest[T, S] = RequestT[PartialU, T, S]

  sealed trait PartialU[A]
  object PartialU {
    case class Method(m: SMethod) extends PartialU[SMethod]
    case class PathQueryFragment(
      path: Seq[String],
      qf: Seq[Uri.QueryFragment],
      f: Option[String]
    ) extends PartialU[Uri]

    implicit def toMethod(pu: PartialU[SMethod]): SMethod = pu
    implicit def fromMethod(m: SMethod): PartialU[SMethod] = Method(m)
    implicit def toPQF(pu: PartialU[Uri]): PathQueryFragment = pu
  }

  def completeRequest[T, S](pr: PartialRequest[T, S], u: Uri): Request[T, S] =
    pr.copy[Id, T, S](
      method = pr.method,
      uri = u.copy(
        path = u.path ++ pr.uri.path,
        queryFragments = pr.uri.qf,
        fragment = pr.uri.f
      )
    )

  object syntax {
    implicit class RequestBuilders[T, S](u: RequestT[Empty, T, S]) {
      def get(path: Seq[String]): PartialRequest[T, S] =
        u.copy[PartialU, T, S](
          uri = PartialU.PathQueryFragment(path, Nil, None),
          method = SMethod.GET
        )
    }
  }
}
