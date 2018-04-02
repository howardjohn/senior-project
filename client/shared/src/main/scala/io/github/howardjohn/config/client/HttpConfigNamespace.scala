package io.github.howardjohn.config.client

import hammock.Codec
import io.circe.{Decoder, Encoder}
import hammock.circe.implicits._
import io.circe.generic.auto._
import io.github.howardjohn.config.Request.{CreateTagRequest, CreateVersionRequest}
import io.github.howardjohn.config._

class HttpConfigNamespace[T: Codec](val namespace: String, http: HttpClient) extends ConfigNamespace[T] {
  def getTag(tag: String): ConfigTag =
    new HttpConfigTag(tag, namespace, http)

  def createTag(tag: String): Result[ConfigTag] =
    http
      .post("tag", CreateTagRequest(tag, namespace))
      .map(_ => getTag(tag))

  def getVersions(): Result[Seq[VersionEntry]] =
    orNotFound(http.get(s"namespace/$namespace"))

  def getVersion(version: String): ConfigVersion[T] =
    new HttpConfigVersion[T](namespace, version, http)

  def createVersion(version: String): Result[ConfigVersion[T]] =
    http
      .post(s"namespace/$namespace/version", CreateVersionRequest(version))
      .map(_ => getVersion(version))
}
