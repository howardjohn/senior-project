package io.github.howardjohn.config.client

import hammock.Codec
import hammock.circe.implicits._
import io.circe.generic.auto._
import io.github.howardjohn.config.Request.CreateNamespaceRequest
import io.github.howardjohn.config._

class HttpConfigDatastore(http: HttpClient) extends ConfigDatastore[Codec] {
  def getNamespace[T: Codec](namespace: String): ConfigNamespace[T] =
    new HttpConfigNamespace[T](namespace, http)

  def createNamespace[T: Codec](namespace: String): Result[ConfigNamespace[T]] =
    http
      .post("namespace", CreateNamespaceRequest(namespace))
      .map(_ => getNamespace(namespace))
}
