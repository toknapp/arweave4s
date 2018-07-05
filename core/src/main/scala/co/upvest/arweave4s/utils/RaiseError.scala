package co.upvest.arweave4s.utils

trait RaiseError[F[_], E] {
  def apply[A](e: E): F[A]
}
