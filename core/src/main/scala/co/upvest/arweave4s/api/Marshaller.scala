package co.upvest.arweave4s.api

import co.upvest.arweave4s.adt.Transaction
import co.upvest.arweave4s.adt._
import co.upvest.arweave4s.utils.{CirceComplaints, EmptyStringAsNone}
import io.circe
import io.circe.{Decoder, HCursor, Encoder, Json, JsonObject}
import io.circe.syntax._
import cats.implicits._

trait Marshaller {
  import CirceComplaints._, EmptyStringAsNone._

  implicit lazy val infoDecoder: Decoder[Info] =
    Decoder.instance { (c: HCursor) =>
      for {
        network   <- c.downField("network").as[String]
        version   <- c.downField("version").as[Int]
        height    <- c.downField("height").as[BigInt]
        blocks    <- c.downField("blocks").as[BigInt]
        peers     <- c.downField("peers").as[Int]
      } yield Info(network, version, height, blocks, peers)
    }

  implicit lazy val peersDecoder: Decoder[Peer] =
    Decoder.instance { (c: HCursor) => c.as[String].map(Peer.apply) }

  implicit lazy val blockHashDecoder: Decoder[Block.Hash] =
    Decoder.instance { (c: HCursor) =>
      (c.as[String] map Block.Hash.fromEncoded) orComplain
    }

  implicit lazy val blockIndepHashDecoder: Decoder[Block.IndepHash] =
    Decoder.instance { (c: HCursor) =>
      c.as[String] map Block.IndepHash.fromEncoded orComplain
    }

  implicit lazy val addressDecoder: Decoder[Address] =
    Decoder.instance { (c: HCursor) =>
      c.as[String] map Address.fromEncoded orComplain
    }

  implicit lazy val winstonDecoder: Decoder[Winston] =
    Decoder.instance { (c: HCursor) =>
      c.as[BigInt] map Winston.apply
    }

  implicit lazy val signatureDecoder: Decoder[Signature] =
    Decoder.instance { (c: HCursor) =>
      c.as[String] map Signature.fromEncoded orComplain
    }

  implicit lazy val ownerDecoder: Decoder[Owner] =
    Decoder.instance { (c: HCursor) =>
      c.as[String] map Owner.fromEncoded orComplain
    }

  implicit lazy val dataDecoder: Decoder[Data] =
    Decoder.instance { (c: HCursor) =>
      c.as[String] map Data.fromEncoded orComplain
    }

  implicit lazy val transactionIdDecoder: Decoder[Transaction.Id] =
    Decoder.instance { c =>
      c.as[String] map Transaction.Id.fromEncoded orComplain
    }

  implicit def base64EncodedBytesEncoder[T <: Base64EncodedBytes]: Encoder[T] =
    Encoder.instance { bs => Json.fromString(bs.toString) }

  implicit lazy val ownerEncoder: Encoder[Owner] =
    Encoder.instance { _.toString.asJson }
  implicit lazy val winstonEncoder: Encoder[Winston] =
    Encoder.instance { _.toString.asJson }
  implicit lazy val transactionTypeEncoder: Encoder[Transaction.Type] =
    Encoder.instance { _.toString.asJson }

  case class NoneAsEmptyStringDecoder[T: Decoder](t: T)

  implicit lazy val dataTransactionDecoder: Decoder[Transaction.Data] =
    Decoder.instance { c =>
      for {
        id     <- c.downField("id").as[Transaction.Id]
        lastTx <- c.downField("last_tx").as[EmptyStringAsNone[Transaction.Id]]
        owner  <- c.downField("owner").as[Owner]
        data   <- c.downField("data").as[Data]
        reward <- c.downField("reward").as[Winston]
      } yield Transaction.Data(id, lastTx, owner, data, reward)
    }

  implicit lazy val dataTransactionEncoder: Encoder[Transaction.Data] =
    Encoder.instance { tx =>
      Json.obj(
        ("id", tx.id.asJson),
        ("last_tx", tx.lastTx.noneAsEmptyString),
        ("target", Json.fromString("")),
        ("owner", tx.owner.asJson),
        ("reward", tx.reward.asJson),
        ("quantity", Winston.Zero.asJson),
        ("data", tx.data.asJson),
        ("type", tx.tpe.asJson)
      )
    }

  implicit lazy val transferTransactionEncoder: Encoder[Transaction.Transfer] =
    Encoder.instance { tx =>
      Json.obj(
        ("id", tx.id.asJson),
        ("last_tx", tx.lastTx.noneAsEmptyString),
        ("data", Json.fromString("")),
        ("owner", tx.owner.asJson),
        ("target", tx.target.asJson),
        ("quantity", tx.quantity.asJson),
        ("reward", tx.reward.asJson),
        ("type", tx.tpe.asJson)
      )
    }

  implicit lazy val transferTransactionDecoder: Decoder[Transaction.Transfer] =
    Decoder.instance { c =>
      for {
        id       <- c.downField("id").as[Transaction.Id]
        lastTx   <- c.downField("last_tx").as[EmptyStringAsNone[Transaction.Id]]
        owner    <- c.downField("owner").as[Owner]
        target   <- c.downField("target").as[Address]
        quantity <- c.downField("quantity").as[Winston]
        reward   <- c.downField("reward").as[Winston]
      } yield Transaction.Transfer(id, lastTx, owner, target, quantity, reward)
    }

  implicit lazy val transactionEncoder: Encoder[Transaction] =
    Encoder.instance {
      case t: Transaction.Transfer => t.asJson
      case t: Transaction.Data => t.asJson
    }

  implicit lazy val transactionDecoder: Decoder[Transaction] =
    Decoder.instance { c =>
      c.downField("type").as[String] >>= { s =>
        Transaction.Type(s) toRight circe.DecodingFailure(
          message = s"unknown transaction type $s",
          ops = Nil
        )
      } >>= {
        case t: Transaction.Type.Transfer.type =>
          transferTransactionDecoder(c)
        case d: Transaction.Type.Data.type =>
          dataTransactionDecoder(c)
      }
    }

  implicit def signedDecoder[T <: Signable: Decoder]: Decoder[Signed[T]] =
    Decoder.instance { c =>
      for {
        sig <- c.downField("signature").as[Signature]
        t   <- c.as[T]
      } yield Signed[T](t, sig)
    }

  implicit def signedEncoder[T <: Signable: Encoder]: Encoder[Signed[T]] =
    Encoder.instance { s =>
      s.t.asJson.withObject { (jo: JsonObject) =>
        Json.fromJsonObject(("signature", s.signature.asJson) +: jo)
      }
    }

  implicit lazy val walletDecoder: Decoder[WalletResponse] =
    Decoder.instance { c =>
      for {
        addr    <- c.downField("wallet").as[Address]
        quant   <- c.downField("quantity").as[Winston]
        last_tx <- c.downField("last_tx").as[EmptyStringAsNone[Transaction.Id]]
      } yield WalletResponse(addr, quant, last_tx)
    }

  implicit lazy val blockDecoder: Decoder[Block] =
    Decoder.instance { c =>
      for {
        nonce         <- c.downField("nonce").as[String]
        prev_block    <- c.downField("previous_block").as[Block.Hash]
        timestamp     <- c.downField("timestamp").as[Long]
        last_retarget <- c.downField("last_retarget").as[Long]
        diff          <- c.downField("diff").as[Int]
        height        <- c.downField("height").as[BigInt]
        hash          <- c.downField("hash").as[Block.Hash]
        indep_hash    <- c.downField("indep_hash").as[Block.IndepHash]
        txs           <- c.downField("txs").as[Seq[Signed[Transaction]]]
        hash_list     <- c.downField("hash_list").as[Seq[Block.Hash]]
        wallet_list   <- c.downField("wallet_list").as[Seq[WalletResponse]]
        reward_addr   <- c.downField("reward_addr").as[String]
      } yield
        Block(
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
    }
}

object Marshaller extends Marshaller
