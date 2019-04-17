package co.upvest.arweave4s.marshalling

import co.upvest.arweave4s.adt.{Transaction, _}
import co.upvest.arweave4s.utils.{CirceComplaints, CryptoUtils, EmptyStringAsNone}
import cats.syntax.option._
import io.circe.syntax._
import cats.syntax.flatMap._
import cats.instances.either._
import io.circe._

import scala.util.Try

trait Marshaller {
  import CirceComplaints._
  import EmptyStringAsNone._


  val mapEmptyString = (s: String) => EmptyStringAsNone.of(s).toOption match {
    case None => Some(None)
    case Some(s) => Transaction.Id.fromEncoded(s) map (_.some)
  }

  val winstonMapper = (s: String) => Try { Winston.apply(s) } toOption

  implicit lazy val infoDecoder: Decoder[Info] = (c: HCursor) => for {
    network   <- c.downField("network").as[String]
    version   <- c.downField("version").as[Int]
    height    <- c.downField("height").as[BigInt]
    current   <- {
      val current  = c.downField("current")
      if (current.as[String].contains("not_joined")) {
        Right(None)
      }
      else {
        current.as[Block.IndepHash] map Some.apply
      }
    }
    blocks    <- c.downField("blocks").as[BigInt]
    peers     <- c.downField("peers").as[Int]
    ql        <- c.downField("queue_length").as[Int]
  } yield Info(
    network = network,
    version = version,
    height = height,
    current = current,
    blocks = blocks,
    peers = peers,
    queueLength = ql
  )

  implicit lazy val peersDecoder: Decoder[Peer] =
    _.as[String] map { Peer(_).toOption } orComplain

  implicit lazy val blockHashDecoder: Decoder[Block.Hash] =
    _.as[String] map Block.Hash.fromEncoded orComplain

  implicit lazy val blockIndepHashDecoder: Decoder[Block.IndepHash] =
    _.as[String] map Block.IndepHash.fromEncoded orComplain

  implicit lazy val addressDecoder: Decoder[Address] =
    _.as[String] map Address.fromEncoded orComplain

  implicit lazy val winstonDecoder: Decoder[Winston] =
    _.as[BigInt] map Winston.apply

  implicit lazy val signatureDecoder: Decoder[Signature] =
    _.as[String] map Signature.fromEncoded orComplain

  implicit lazy val ownerDecoder: Decoder[Owner] =
    _.as[String] map Owner.fromEncoded orComplain

  implicit lazy val dataDecoder: Decoder[Data] =
    _.as[String] map Data.fromEncoded orComplain

  implicit lazy val transactionIdDecoder: Decoder[Transaction.Id] =
    _.as[String] map Transaction.Id.fromEncoded orComplain

  implicit def base64EncodedBytesEncoder[T <: Base64EncodedBytes]: Encoder[T] =
    bs => Json.fromString(bs.toString)

  implicit lazy val ownerEncoder: Encoder[Owner]     = _.toString.asJson
  implicit lazy val winstonEncoder: Encoder[Winston] = _.toString.asJson

  case class NoneAsEmptyStringDecoder[T: Decoder](t: T)

  object TagsInTransaction {
    lazy implicit val encoder: Encoder[Tag.Custom] = ct =>
      Json.obj(
        "name"  := CryptoUtils.base64UrlEncode(ct.name),
        "value" := CryptoUtils.base64UrlEncode(ct.value)
      )

    lazy implicit val decoder: Decoder[Tag.Custom] = c => for {
      n <- (c.downField("name").as[String]
        map CryptoUtils.base64UrlDecode
        orComplain
      )
      v <- (c.downField("value").as[String]
        map CryptoUtils.base64UrlDecode
        orComplain
      )
    } yield Tag.Custom(name = n, value = v)
  }

  implicit lazy val queryEncoder: Encoder[Query] = {
    case Query.Or(q1, q2) =>
      Json.obj(
        "op"    := "or",
        "expr1" := q1,
        "expr2" := q2
      )
    case Query.And(q1, q2) =>
      Json.obj(
        "op"    := "and",
        "expr1" := q1,
        "expr2" := q2
      )
    case Query.Exact(Tag.From(a)) =>
      Json.obj(
        "op"    := "equals",
        "expr1" := CryptoUtils.base64UrlEncode("from".getBytes),
        "expr2" := a.toString
      )
    case Query.Exact(Tag.To(a)) =>
      Json.obj(
        "op"    := "equals",
        "expr1" := CryptoUtils.base64UrlEncode("to".getBytes),
        "expr2" := a.toString
      )
    case Query.Exact(ct: Tag.Custom) =>
      Json.obj(
        "op"    := "equals",
        "expr1" := CryptoUtils.base64UrlEncode(ct.name),
        "expr2" := CryptoUtils.base64UrlEncode(ct.value)
      )
  }

  implicit lazy val transactionDecoder: Decoder[Transaction] = c => for {
    lastTx <- c.downField("last_tx").as[EmptyStringAsNone[Transaction.Id]]
    owner  <- c.downField("owner").as[Owner]
    reward <- c.downField("reward").as[Winston]
    data   <- c.downField("data").as[EmptyStringAsNone[Data]]
    tags   <- {
      import TagsInTransaction.decoder
      c.downField("tags").as[Option[Seq[Tag.Custom]]]
    }
    target   <- c.downField("target").as[EmptyStringAsNone[Address]]
    quantity <- c.downField("quantity").as[Winston]
  } yield Transaction(lastTx, owner, reward, data, tags, target, quantity)

  implicit lazy val transactionEncoder: Encoder[Signed[Transaction]] = tx =>
    Json.fromFields(
      List(
        "id"        := tx.id,
        "last_tx"   -> tx.lastTx.noneAsEmptyString,
        "reward"    := tx.reward,
        "owner"     := tx.owner,
        "signature" := tx.signature,
        "data"      := tx.data.noneAsEmptyString,
        "target"    := tx.target.noneAsEmptyString,
        "quantity"  := tx.quantity,
        "tags"      -> {
          import TagsInTransaction.encoder
          tx.tags.toSeq.flatten.asJson
        }
      )
    )

  implicit def signedDecoder[T <: Signable: Decoder]: Decoder[Signed[T]] =
    c =>
      for {
        sig <- c.downField("signature").as[Signature]
        t   <- c.as[T]
      } yield Signed[T](t, sig)

  implicit lazy val blockDecoder: Decoder[Block] = c =>
    for {
      nonce         <- c.downField("nonce").as[String]
      prev_block    <- c.downField("previous_block").as[EmptyStringAsNone[Block.IndepHash]]
      timestamp     <- c.downField("timestamp").as[Long]
      last_retarget <- c.downField("last_retarget").as[Long]
      diff          <- c.downField("diff").as[Int]
      height        <- c.downField("height").as[BigInt]
      hash          <- c.downField("hash").as[Block.Hash]
      indep_hash    <- c.downField("indep_hash").as[Block.IndepHash]
      txs           <- c.downField("txs").as[Seq[Transaction.Id]]
      rewaddr       =  c.downField("reward_addr")
      reward_addr   <- rewaddr.as[String] >>= {
        case "unclaimed" => Right(None)
        case _ => rewaddr.as[Address] map Some.apply
      }
    } yield Block(
      nonce = nonce,
      previousBlock = prev_block,
      timestamp = timestamp,
      lastRetarget = last_retarget,
      diff = diff,
      height = height,
      hash = hash,
      indepHash = indep_hash,
      txs = txs,
      rewardAddr = reward_addr
    )

  implicit lazy val blockEncoder: Encoder[Block] = b => Json.obj(
    "nonce"          := b.nonce,
    "previous_block" -> b.previousBlock.noneAsEmptyString,
    "timestamp"      := b.timestamp,
    "last_retarget"  := b.lastRetarget,
    "diff"           := b.diff,
    "height"         := b.height,
    "hash"           := b.hash,
    "indep_hash"     := b.indepHash,
    "txs"            := b.txs,
    "reward_addr"    -> (b.rewardAddr map {_.asJson} getOrElse "unclaimed".asJson)
  )
}

object Marshaller extends Marshaller
