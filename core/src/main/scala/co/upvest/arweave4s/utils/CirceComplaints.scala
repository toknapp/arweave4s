package co.upvest.arweave4s.utils

import io.circe.{Decoder, DecodingFailure}

trait CirceComplaints {
  implicit class OptionComplainer[T](o: Option[T]) {
    def orComplain: Decoder.Result[T] =
      o toRight DecodingFailure("invalid encoding", Nil)
  }

  implicit class DecoderResultComplainer[T](d: Decoder.Result[Option[T]]) {
    def orComplain: Decoder.Result[T] = d map { _ orComplain } joinRight
  }
}

object CirceComplaints extends CirceComplaints
