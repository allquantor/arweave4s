package co.upvest.arweave4s.api.v1.marshalling

import co.upvest.arweave4s.adt.Transaction
import co.upvest.arweave4s.adt._
import io.circe.Decoder.Result
import io.circe.{Decoder, HCursor, DecodingFailure}

trait MarshallerV1 {

  implicit lazy val infoDecoder: Decoder[Info] = new Decoder[Info] {
    override def apply(c: HCursor): Result[Info] =
      for {
        network <- c.downField("network").as[String]
        version <- c.downField("version").as[Int]
        height  <- c.downField("height").as[BigInt]
        blocks  <- c.downField("blocks").as[BigInt]
        peers   <- c.downField("peers").as[Int]
      } yield Info(network, version, height, blocks, peers)
  }

  implicit lazy val peersDecoder: Decoder[Peer] =
    (c: HCursor) => c.as[String].map(Peer.apply)

  implicit class DecoderComplainer[T](d: Decoder.Result[Option[T]]) {
    def orComplain: Decoder.Result[T] = d map {
      _ toRight DecodingFailure("invalid encoding", Nil)
    } joinRight
  }

  implicit lazy val blockHashDecoder: Decoder[Block.Hash] =
    (c: HCursor) => (c.as[String] map Block.Hash.fromEncoded) orComplain

  implicit lazy val blockIndepHashDecoder: Decoder[Block.IndepHash] =
    (c: HCursor) => c.as[String] map Block.IndepHash.fromEncoded orComplain

  implicit lazy val addressDecoder: Decoder[Address] =
    (c: HCursor) => c.as[String] map Address.fromEncoded orComplain

  implicit lazy val winstonDecoder: Decoder[Winston] =
    (c: HCursor) => c.as[BigInt] map Winston

  implicit lazy val signatureDecoder: Decoder[Signature] =
    (c: HCursor) => c.as[String] map Signature.fromEncoded orComplain

  implicit lazy val ownerDecoder: Decoder[Owner] =
    (c: HCursor) => c.as[String] map Owner.fromEncoded orComplain

  implicit lazy val dataDecoder: Decoder[Data] =
    (c: HCursor) => c.as[String] map Data.fromEncoded orComplain

  implicit lazy val transactionIdDecoder: Decoder[Transaction.Id] =
    (c: HCursor) => c.as[String] map Transaction.Id.fromEncoded orComplain

  implicit lazy val dataTransactionDecoder = new Decoder[Transaction.Data] {
    override def apply(c: HCursor): Result[Transaction.Data] =
      for {
        id       <- c.downField("id").as[Transaction.Id]
        lastTx   <- c.downField("last_tx").as[Option[Transaction.Id]] // TODO: ensure "" is interpreted as None
        owner    <- c.downField("owner").as[Owner]
        data      <- c.downField("data").as[Data]
        reward    <- c.downField("reward").as[Winston]
      } yield Transaction.Data(id, lastTx, owner, data, reward)
  }

  implicit lazy val transferTransactionDecoder =
    new Decoder[Transaction.Transfer] {
      override def apply(c: HCursor): Result[Transaction.Transfer] =
        for {
          id       <- c.downField("id").as[Transaction.Id]
          lastTx   <- c.downField("last_tx").as[Option[Transaction.Id]] // TODO: ensure "" is interpreted as None
          owner    <- c.downField("owner").as[Owner]
          target   <- c.downField("target").as[Address]
          quantity <- c.downField("quantity").as[Winston]
          reward    <- c.downField("reward").as[Winston]
        } yield Transaction.Transfer(id, lastTx, owner, target, quantity, reward)
    }

  implicit lazy val transactionDecoder = new Decoder[Transaction] {
    override def apply(c: HCursor): Result[Transaction] =
      c.downField("type").as[String] flatMap { s =>
        Transaction.Type(s) toRight DecodingFailure(
          message = s"unknown transaction type $s",
          ops = Nil
        )
      } flatMap {
        case t: Transaction.Type.Transfer.type =>
          transferTransactionDecoder(c)
        case d: Transaction.Type.Data.type =>
          dataTransactionDecoder(c)
      }
  }

  implicit def signedDecoder[T <: Signable : Decoder]: Decoder[Signed[T]] = (c: HCursor) =>
    for {
      sig <- c.downField("signature").as[Signature]
      t <- c.as[T]
    } yield Signed[T](t, sig)

  implicit lazy val walletDecoder = new Decoder[WalletResponse] {
    override def apply(c: HCursor): Result[WalletResponse] =
      for {
        addr    <- c.downField("wallet").as[Address]
        quant   <- c.downField("quantity").as[Winston]
        last_tx <- c.downField("last_tx").as[Option[Transaction.Id]] // TODO: ensure "" is interpreted as None
      } yield WalletResponse(addr, quant, last_tx)
  }

  implicit lazy val blockDecoder = new Decoder[Block] {
    override def apply(c: HCursor): Result[Block] =
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
          indep_hash = indep_hash,
          txs = txs,
          hashList = hash_list,
          walletList = wallet_list,
          rewardAddr = reward_addr
        )
  }
}
