package io.github.howardjohn.core.config

import io.github.howardjohn.core.{Result, TagEntry}

trait ConfigTag {
  def tagName: String

  def getDetails(namespace: String): Result[Option[TagEntry]]
  def getDetails(namespace: String, discriminator: String): Result[Option[TagEntry]]

  def moveTag(namespace: String, version: String): Result[Unit]
}
