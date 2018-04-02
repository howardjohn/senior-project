package io.github.howardjohn.config.client

import hammock.Codec
import hammock.circe.implicits._
import io.circe.generic.auto._
import io.github.howardjohn.config.Request.FreezeVersionRequest
import io.github.howardjohn.config._

class HttpConfigVersion[T: Codec](val namespace: String, val version: String, http: HttpClient)
    extends ConfigVersion[T] {
  private val baseUrl = s"namespace/$namespace/version/$version"

  def details(): Result[Option[VersionEntry]] =
    http.get(baseUrl)

  def freeze(): Result[Unit] =
    orNotFound(http.put(baseUrl, FreezeVersionRequest(true)))

  def cloneVersion(newVersionName: String): Result[ConfigVersion[T]] = ???

  def get(key: String): Result[Option[ConfigEntry[T]]] =
    http.get(s"$baseUrl/config/$key")

  def getAll(): Result[Seq[ConfigEntry[T]]] =
    orNotFound(http.get(s"$baseUrl/config"))

  def write(key: String, value: T): Result[Unit] =
    orNotFound(http.post(s"$baseUrl/config/$key", value))
      .map(_ => ())

  def delete(key: String): Result[Unit] =
    orNotFound(http.delete(s"$baseUrl/config/$key"))
}
