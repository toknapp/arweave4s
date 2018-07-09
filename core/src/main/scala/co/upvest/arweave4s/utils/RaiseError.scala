package co.upvest.arweave4s.utils

import cats.MonadError
import cats.data.NonEmptyList

trait RaiseError[F[_], E] {
  def apply[A](e: E): F[A]
}

object RaiseError {
  implicit def fromMonadError[F[_], E](implicit
    M: MonadError[F, E]
  ): RaiseError[F, E] = new RaiseError[F, E] {
    def apply[A](e: E) = M raiseError e
  }

  implicit def nelView[F[_], E](
    implicit me: MonadError[F, E],
    view: NonEmptyList[Throwable] => E
  ) = new RaiseError[F, NonEmptyList[Throwable]] {
    def apply[A](nel: NonEmptyList[Throwable]): F[A] = me raiseError view(nel)
  }
}
