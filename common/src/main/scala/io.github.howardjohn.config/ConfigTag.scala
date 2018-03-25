package io.github.howardjohn.config

trait ConfigTag {
  def tagName: String

  def getDetails(namespace: String): Result[Option[TagEntry]]
  def getDetails(namespace: String, discriminator: String): Result[Option[TagEntry]]

  def moveTag(namespace: String, versions: Map[String, Int]): Result[Unit]
}
