package io.github.howardjohn.config.client

import cats.Id
import hammock._
import hammock.circe.implicits._
import io.circe.generic.auto._
import io.github.howardjohn.config._

class HttpConfigDatastore(http: HttpClient) extends ConfigDatastore[Id] {
  def getNamespace[T: Id](namespace: String): ConfigNamespace[T] =
    new HttpConfigNamespace[T](namespace, http)

  def getTag(tag: String): ConfigTag =
    new HttpConfigTag(tag, http)

  def createNamespace[T: Id](namespace: String): Result[ConfigNamespace[T]] =
    http
      .post("namespace", Map("namespace" -> namespace))
      .map(_ => getNamespace(namespace))
  def createTag(tag: String, namespace: String): Result[ConfigTag] = ???
}
