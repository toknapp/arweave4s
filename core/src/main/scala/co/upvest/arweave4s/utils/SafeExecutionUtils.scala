package co.upvest.arweave4s.utils

import cats.data.EitherT
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

object SafeExecutionUtils {

  implicit class ToExecute[T](any: => T) {

    def safeExecute(implicit ec: ExecutionContext): EitherT[Future, Throwable, T] =
      EitherT(Future(Try(any).toEither))

    def fastUnSafeExecute: EitherT[Future, Throwable, T] =
      EitherT(Future.successful(Right(any): Either[Throwable, T]))
  }

}
