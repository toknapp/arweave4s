package co.upvest.arweave4s.api

import co.upvest.arweave4s.adt.Transaction
import co.upvest.arweave4s.adt._
import co.upvest.arweave4s.utils.{CirceComplaints, CryptoUtils, EmptyStringAsNone}
import io.circe.Decoder.Result
import io.circe
import io.circe.{Decoder, Encoder, HCursor, Json}
import io.circe.syntax._

import scala.util.Try

trait Marshaller {
  import CirceComplaints._, EmptyStringAsNone._

   val mapEmptyTxId  = (s: String) => EmptyStringAsNone.of(s).toOption match {
    case None => Some(None)
    case Some(s) => Transaction.Id.fromEncoded(s) map Some.apply
  }

  val mapEmptyString = (s: String) => EmptyStringAsNone.of(s).toOption match {
    case None => Some(None)
    case Some(s) => Transaction.Id.fromEncoded(s) map Some.apply
  }

  val winstonMapper = (s: String) => Try { Winston.apply(s) } toOption

  implicit lazy val infoDecoder: Decoder[Info] = (c: HCursor) => for {
    network   <- c.downField("network").as[String]
    version   <- c.downField("version").as[Int]
    height    <- c.downField("height").as[BigInt]
    current   <- c.downField("current").as[Block.IndepHash]
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
    _.as[String].map(Peer.apply)

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

  implicit lazy val dataTransactionDecoder = new Decoder[Transaction.Data] {
    override def apply(c: HCursor): Result[Transaction.Data] =
      for {
        lastTx <- c.downField("last_tx").as[EmptyStringAsNone[Transaction.Id]]
        owner  <- c.downField("owner").as[Owner]
        data   <- c.downField("data").as[Data]
        reward <- c.downField("reward").as[Winston]
        tags   <- {
          import TagsInTransaction.decoder
          c.downField("tags").as[Seq[Tag.Custom]]
        }
      } yield Transaction.Data(lastTx, owner, data, reward, tags)
  }

  implicit lazy val dataTransactionEncoder: Encoder[Signed[Transaction.Data]] =
    tx =>
      Json.obj(
        ("id", tx.id.asJson),
        ("last_tx", tx.lastTx.noneAsEmptyString),
        ("target", Json.fromString("")),
        ("owner", tx.owner.asJson),
        ("reward", tx.reward.asJson),
        ("quantity", Winston.Zero.asJson),
        ("data", tx.data.asJson),
        ("tags", {
          import TagsInTransaction.encoder
          tx.tags asJson
        }),
        ("signature", tx.signature.asJson)
      )

  implicit lazy val transferTransactionEncoder: Encoder[Signed[Transaction.Transfer]] =
    tx =>
      Json.obj(
        ("id", tx.id.asJson),
        ("last_tx", tx.lastTx.noneAsEmptyString),
        ("data", Json.fromString("")),
        ("owner", tx.owner.asJson),
        ("target", tx.target.asJson),
        ("quantity", tx.quantity.asJson),
        ("reward", tx.reward.asJson),
        ("signature", tx.signature.asJson)
    )

  implicit lazy val transactionEncoder: Encoder[Signed[Transaction]] = stx =>
    stx.t match {
      case t: Transaction.Transfer => stx.copy(t = t).asJson
      case t: Transaction.Data => stx.copy(t = t).asJson
    }

  implicit lazy val transferTransactionDecoder =
    new Decoder[Transaction.Transfer] {
      override def apply(c: HCursor): Result[Transaction.Transfer] =
        for {
          lastTx   <- c.downField("last_tx").as[EmptyStringAsNone[Transaction.Id]]
          owner    <- c.downField("owner").as[Owner]
          target   <- c.downField("target").as[Address]
          quantity <- c.downField("quantity").as[Winston]
          reward   <- c.downField("reward").as[Winston]
        } yield Transaction.Transfer(lastTx, owner, target, quantity, reward)
    }

  implicit lazy val transactionDecoder = new Decoder[Transaction] {
    override def apply(c: HCursor): Result[Transaction] =
      (for {
        d <- c.downField("data").as[EmptyStringAsNone[Data]]
        q <- c.downField("quantity").as[Option[Winston]]
      } yield (d.toOption, q)) flatMap {
        case (None, Some(_)) => transferTransactionDecoder(c)
        case (Some(_), Some(Winston.Zero)) => dataTransactionDecoder(c)
        case _ => Left(circe.DecodingFailure(
          message = s"unknown transaction type",
          ops = Nil
        ))
      }
  }

  implicit def signedDecoder[T <: Signable: Decoder]: Decoder[Signed[T]] =
    c =>
      for {
        sig <- c.downField("signature").as[Signature]
        t   <- c.as[T]
      } yield Signed[T](t, sig)

  implicit lazy val walletDecoder: Decoder[WalletResponse] = c => for {
    addr    <- c.downField("wallet").as[Address]
    quant   <- c.downField("quantity").as[Winston]
    last_tx <- c.downField("last_tx").as[EmptyStringAsNone[Transaction.Id]]
  } yield WalletResponse(addr, quant, last_tx)

  implicit lazy val walletEncoder: Encoder[WalletResponse] = w => Json.obj(
    "wallet" := w.address,
    "quantity" := w.quantity,
    "last_tx" -> w.last_tx.noneAsEmptyString
  )

  implicit lazy val blockDecoder: Decoder[Block] = c =>
    for {
      nonce         <- c.downField("nonce").as[String]
      prev_block    <- c.downField("previous_block").as[Block.IndepHash]
      timestamp     <- c.downField("timestamp").as[Long]
      last_retarget <- c.downField("last_retarget").as[Long]
      diff          <- c.downField("diff").as[Int]
      height        <- c.downField("height").as[BigInt]
      hash          <- c.downField("hash").as[Block.Hash]
      indep_hash    <- c.downField("indep_hash").as[Block.IndepHash]
      txs           <- c.downField("txs").as[Seq[Transaction.Id]]
      hash_list     <- c.downField("hash_list").as[Seq[Block.Hash]]
      wallet_list   <- c.downField("wallet_list").as[Seq[WalletResponse]]
      rewaddr       =  c.downField("reward_addr")
      reward_addr   <- rewaddr.as[String] flatMap {
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
      hashList = hash_list,
      walletList = wallet_list,
      rewardAddr = reward_addr
    )

  implicit lazy val blockEncoder: Encoder[Block] = b => Json.obj(
    "nonce" := b.nonce,
    "previous_block" := b.previousBlock,
    "timestamp" := b.timestamp,
    "last_retarget" := b.lastRetarget,
    "diff" := b.diff,
    "height" := b.height,
    "hash" := b.hash,
    "indep_hash" := b.indepHash,
    "txs" := b.txs,
    "hash_list" := b.hashList,
    "wallet_list" := b.walletList,
    "reward_addr" -> (b.rewardAddr map {_.asJson} getOrElse "unclaimed".asJson)
  )
}

object Marshaller extends Marshaller
