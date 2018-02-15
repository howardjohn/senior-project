package io.github.howardjohn.core.config

import io.circe.Json
import io.github.howardjohn.core.config.ConfigDatastore.{ConfigEntry, Result}

trait ConfigVersion {
  def version: String
  def namespace: String

  def cloneVersion(newVersionName: String): Result[ConfigVersion]

  def get(key: String): Result[Option[ConfigEntry]]
  def getAll(): Result[Seq[ConfigEntry]]
  def write(key: String, value: Json): Result[Unit]
  def delete(key: String): Result[Unit]
}
