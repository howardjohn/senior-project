package io.github.howardjohn.config.client

import hammock.circe.implicits._
import io.circe.generic.auto._
import io.github.howardjohn.config._

class HttpConfigTag(val tagName: String, val namespace: String, http: HttpClient) extends ConfigTag {
  private val baseUrl = s"tag/$tagName/namespace/$namespace"

  def getDetails(): Result[Option[TagEntry]] =
    http.get(baseUrl)

  def getDetails(discriminator: String): Result[Option[TagEntry]] =
    http.get(s"$baseUrl?discriminator=$discriminator")

  def moveTag(versions: Map[String, Int]): Result[Unit] =
    orNotFound(http.put(baseUrl, versions))
}
