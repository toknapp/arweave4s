package co.upvest.arweave4s.utils

import cats.Functor
import co.upvest.arweave4s.api.AbstractConfig
import com.softwaremill.sttp.circe.asJson
import com.softwaremill.sttp.{Request, Response, Uri}
import io.circe.Decoder

import scala.util.Random

object RequestHandling {

  type RespF[F[_], A] = F[Response[Either[io.circe.Error, A]]]

  def process[F[_]: Functor, G[_], A: Decoder](p: List[String], f: Uri => Request[String, Nothing])
                                     (implicit c: AbstractConfig[F, G]): List[RespF[F, A]] = {
    import cats.syntax.functor._

    lazy val permute = (u: Uri) => List.fill(c.retries)(u.copy(path = u.path ++ p))
    lazy val send    = (u: Uri) => c.i(c.backend send f(u).response(asJson[A]))

    Random.shuffle(
      c.uris.toStream
        .flatMap(permute)
        .map(send)
    ).foldLeft(List.empty[RespF[F, A]]) {
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
