package io.github.howardjohn.config

trait ConfigTag {
  def tagName: String
  def namespace: String

  def getDetails(): Result[Option[TagEntry]]
  def getDetails(discriminator: String): Result[Option[TagEntry]]

  def moveTag(versions: Map[String, Int]): Result[Unit]
}
