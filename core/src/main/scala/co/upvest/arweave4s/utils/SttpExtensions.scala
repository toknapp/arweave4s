package co.upvest.arweave4s.utils

import cats.Id
import com.softwaremill.sttp.{Empty, Request, RequestT, Uri, Method => SMethod}

import scala.collection.immutable.Seq

object SttpExtensions {

  type PartialRequest[T, S] = RequestT[PartialU, T, S]

  sealed trait PartialU[A]

  object PartialU {
    case class Method(m: SMethod) extends PartialU[SMethod]
    case class PathQueryFragment(
      path: Seq[String],
      qf: Seq[Uri.QueryFragment],
      f: Option[String]
    ) extends PartialU[Uri]

    implicit def fromMethod(m: SMethod): PartialU[SMethod] = Method(m)
    implicit def toMethod(pu: PartialU[SMethod]): SMethod = pu match { case Method(m) => m }
    implicit def toPQF(pu: PartialU[Uri]): PathQueryFragment = pu match { case p : PathQueryFragment => p }
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
            method = PartialU.Method(SMethod.GET)
          )

        def post(path: Seq[String]): PartialRequest[T, S] =
          u.copy[PartialU, T, S](
            uri = PartialU.PathQueryFragment(path, Nil, None),
            method = PartialU.Method(SMethod.POST)
          )
      }
    }
}
