package io.github.howardjohn.config.client

import io.circe.{Decoder, Encoder, HCursor, Json}

trait JsonCodec[A] extends Encoder[A] with Decoder[A]

object JsonCodec {
  def apply[A](implicit instance: JsonCodec[A]): JsonCodec[A] = instance

  def instance[A](encode: Encoder[A], decode: Decoder[A]): JsonCodec[A] =
    new JsonCodec[A] {
      private val enc = encode
      private val dec = decode
      override def apply(a: A): Json = enc(a)
      override def apply(c: HCursor): Decoder.Result[A] = dec(c)
    }

  implicit def summonCodecFromEncoderAndDecoder[A](implicit encode: Encoder[A], decode: Decoder[A]): JsonCodec[A] =
    JsonCodec.instance(encode, decode)
}
