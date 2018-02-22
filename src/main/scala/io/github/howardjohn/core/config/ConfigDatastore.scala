package io.github.howardjohn.core.config

import java.time.Instant

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

  case class AuditInfo(
    createdTime: Option[Long] = None,
    modifiedTime: Option[Long] = None
  )

  object AuditInfo{
    def default(getNow: => Long = Instant.now.toEpochMilli): AuditInfo = {
      val now = getNow
      AuditInfo(Some(now), Some(now))
    }
  }
}