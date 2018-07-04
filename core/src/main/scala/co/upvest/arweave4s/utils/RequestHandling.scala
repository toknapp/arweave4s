package co.upvest.arweave4s.utils

import cats.Functor
import co.upvest.arweave4s.api.AbstractConfig
import com.softwaremill.sttp.{RequestT, Response, Uri}


import scala.util.{Random => r}


object RequestHandling {

  type RespF[F[_], A] = F[Response[Either[io.circe.Error, A]]]

  def process[F[_] : Functor, G[_], T, S](p: List[String],
                                                fReq: Uri => RequestT[cats.Id, T, S]
                                               )
                                               (implicit c: AbstractConfig[F, G]): List[RespF[F, A]] = {
    import cats.syntax.functor._
    lazy val permute = (u: Uri) => List.fill(c.retries)(u.copy(path = u.path ++ p))
    lazy val send = (u: Uri) => c.i(c.backend send fReq(u))


    r.shuffle(c.uris.toStream.flatMap(permute))
      .map(send)
      .foldLeft(List.empty[RespF[F, A]]) {
        (b, a) =>
          a.map { rr =>
            rr.body match {
              case Right(_) =>
                return a :: b
              case Left(_) =>
            }
          }
          a :: b
      }
  }
}
