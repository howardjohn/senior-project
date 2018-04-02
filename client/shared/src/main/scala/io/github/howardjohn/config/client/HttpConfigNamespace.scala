package io.github.howardjohn.config.client

import hammock.circe.implicits._
import io.circe.generic.auto._
import io.github.howardjohn.config.Request.CreateTagRequest
import io.github.howardjohn.config._

class HttpConfigNamespace[T](val namespace: String, http: HttpClient) extends ConfigNamespace[T] {
  def getTag(tag: String): ConfigTag =
    new HttpConfigTag(tag, namespace, http)

  def createTag(tag: String): Result[ConfigTag] =
    http
      .post("tag", CreateTagRequest(tag, namespace))
      .map(_ => getTag(tag))

  def getVersions(): Result[Seq[VersionEntry]] = ???

  def getVersion(version: String): ConfigVersion[T] = ???

  def createVersion(version: String): Result[ConfigVersion[T]] = ???
}
