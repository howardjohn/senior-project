package io.github.howardjohn.config.client

import io.github.howardjohn.config.{ConfigTag, Result, TagEntry}

class HttpConfigTag(val tagName: String, http: HttpClient) extends ConfigTag {
  def getDetails(namespace: String): Result[Option[TagEntry]] = ???
  def getDetails(namespace: String, discriminator: String): Result[Option[TagEntry]] = ???

  def moveTag(namespace: String, versions: Map[String, Int]): Result[Unit] = ???

}
