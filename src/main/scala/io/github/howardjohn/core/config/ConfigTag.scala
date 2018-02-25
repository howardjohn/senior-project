package io.github.howardjohn.core.config

import io.github.howardjohn.core.{Result, TagEntry}

trait ConfigTag {
  def tagName: String
  def namespace: String

  def getDetails(): Result[Option[TagEntry]]
}
