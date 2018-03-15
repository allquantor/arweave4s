package co.upvest.arweave4s.adt

import co.upvest.arweave4s.utils.CryptoUtils

class Signature(val bytes: Array[Byte]) extends Base64EncodedBytes

object Signature {
  def fromEncoded(s: String) = new Signature(CryptoUtils.base64UrlDecode(s))
}
