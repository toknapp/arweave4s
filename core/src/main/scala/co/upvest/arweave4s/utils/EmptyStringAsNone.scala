package co.upvest.arweave4s.utils

import io.circe.{Decoder, Encoder, Json, JsonObject}
import io.circe.syntax._
import cats.instances.either._
import cats.syntax.flatMap._
import cats.syntax.option._
import cats.syntax.applicative._

case class EmptyStringAsNone[T](toOption: Option[T]) extends AnyVal

trait EmptyStringAsNoneImplicits {
  implicit def castEmptyStringAsNone[T](e: EmptyStringAsNone[T]): Option[T] =
    e.toOption

  implicit class EmptyStringAsNoneEncoderSyntax[T: Encoder](
    ot: Option[T]
  ) {
    def noneAsEmptyString: Json = ot match {
      case None => JsonObject.empty.asJson
      case Some(t) => t.asJson
    }
  }

  implicit def emptyStringAsNoneDecoder[T: Decoder]: Decoder[EmptyStringAsNone[T]] = c =>
    c.as[String] >>= {
      case "" => EmptyStringAsNone(None).pure
      case _ => c.as[T] map { (t: T) => EmptyStringAsNone(t.some) }
    }
}

object EmptyStringAsNone extends EmptyStringAsNoneImplicits {
  def of(s: String): EmptyStringAsNone[String] = s match {
    case "" => EmptyStringAsNone(None)
    case _ => EmptyStringAsNone(s.some)
  }
}
