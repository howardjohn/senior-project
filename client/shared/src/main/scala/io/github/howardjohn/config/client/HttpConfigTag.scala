package io.github.howardjohn.config.client

import io.github.howardjohn.config.{ConfigTag, Result, TagEntry}

class HttpConfigTag(val tagName: String, val namespace: String, http: HttpClient) extends ConfigTag {
  def getDetails(): Result[Option[TagEntry]] = ???
  def getDetails(discriminator: String): Result[Option[TagEntry]] = ???

  def moveTag(versions: Map[String, Int]): Result[Unit] = ???

}
