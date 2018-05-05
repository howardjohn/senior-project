package io.github.howardjohn.config.client

import hammock.circe.implicits._
import io.circe.generic.auto._
import io.github.howardjohn.config.Request.CreateNamespaceRequest
import io.github.howardjohn.config._

class HttpConfigDatastore(http: HttpClient) extends ConfigDatastore[JsonCodec] {
  def getNamespace[T: JsonCodec](namespace: String): ConfigNamespace[T] =
    new HttpConfigNamespace[T](namespace, http)

  def getAllNamespaces(): Result[Seq[String]] =
    http.get[Seq[String]]("namespace").map(_.getOrElse(Seq.empty))

  def createNamespace[T: JsonCodec](namespace: String): Result[ConfigNamespace[T]] =
    http
      .post("namespace", CreateNamespaceRequest(namespace))
      .map(_ => getNamespace(namespace))
}
