package io.github.howardjohn.core.config

import cats.effect.IO
import io.circe.Json

trait ConfigDatastore {
  import ConfigDatastore._
  def createNamespace(namespace: String): Result[ConfigNamespace]
  def getNamespace(namespace: String): ConfigNamespace
}

object ConfigDatastore {
  type Result[A] = IO[Either[ConfigError, A]]

  case class ConfigEntry(
    key: String,
    version: String,
    value: Json
  )
}