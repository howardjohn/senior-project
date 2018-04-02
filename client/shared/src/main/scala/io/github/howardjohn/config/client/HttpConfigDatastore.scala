package io.github.howardjohn.config.client

import cats.Id
import hammock.circe.implicits._
import io.circe.generic.auto._
import io.github.howardjohn.config.Request.CreateNamespaceRequest
import io.github.howardjohn.config._

class HttpConfigDatastore(http: HttpClient) extends ConfigDatastore[Id] {
  def getNamespace[T: Id](namespace: String): ConfigNamespace[T] =
    new HttpConfigNamespace[T](namespace, http)

  def createNamespace[T: Id](namespace: String): Result[ConfigNamespace[T]] =
    http
      .post("namespace", CreateNamespaceRequest(namespace))
      .map(_ => getNamespace(namespace))
}
