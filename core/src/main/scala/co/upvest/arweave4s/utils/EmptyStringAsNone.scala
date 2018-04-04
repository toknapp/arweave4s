package co.upvest.arweave4s.utils

import io.circe.{Decoder, Encoder, Json}
import io.circe.syntax._
import cats.instances.either._
import cats.syntax.flatMap._
import cats.syntax.applicative._

case class EmptyStringAsNone[T](toOption: Option[T]) extends AnyVal

trait EmptyStringAsNoneImplicits {
  implicit def castEmptyStringAsNone[T](e: EmptyStringAsNone[T]): Option[T] =
    e.toOption

  implicit class EmptyStringAsNoneEncoderSyntax[T: Encoder](
    ot: Option[T]
  ) {
    def noneAsEmptyString: Json = ot match {
      case None => Json.fromString("")
      case Some(t) => t.asJson
    }
  }

  implicit def emptyStringAsNoneDecoder[T: Decoder]: Decoder[EmptyStringAsNone[T]] =
    Decoder.instance {
      c => c.as[String] >>= {
        case "" => EmptyStringAsNone(None).pure
        case _ => c.as[T].right map { (t: T) => EmptyStringAsNone(Some(t)) }
      }
    }
}

object EmptyStringAsNone extends EmptyStringAsNoneImplicits {
  def of(s: String): EmptyStringAsNone[String] = s match {
    case "" => EmptyStringAsNone(None)
    case _ => EmptyStringAsNone(Some(s))
  }
}
