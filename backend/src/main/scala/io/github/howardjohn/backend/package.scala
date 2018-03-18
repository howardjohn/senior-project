package io.github.howardjohn

import java.time.Instant

import cats.data.EitherT
import cats.effect.IO
import io.circe.Json

package object backend {
  type Result[A] = EitherT[IO, ConfigError, A]

  case class ConfigEntry(
    key: String,
    version: String,
    value: Json,
    auditInfo: AuditInfo
  )

  case class VersionEntry(
    namespace: String,
    version: String,
    frozen: Boolean,
    auditInfo: AuditInfo
  )

  case class TagEntry(
    tag: String,
    namespace: String,
    version: String,
    auditInfo: AuditInfo
  )

  case class AuditInfo(
    createdTime: Option[Long] = None,
    modifiedTime: Option[Long] = None
  )

  sealed trait ConfigError

  object ConfigError {
    case object IllegalWrite extends ConfigError
    case object FrozenVersion extends ConfigError
    case object MissingEntry extends ConfigError
    case object MissingDiscriminator extends ConfigError
    case object ExtraDiscriminator extends ConfigError
    case class ReadError(cause: String) extends ConfigError
    case class UnknownError(cause: String) extends ConfigError
  }

  object AuditInfo {

    def default(getNow: => Long = Instant.now.toEpochMilli): AuditInfo = {
      val now = getNow
      AuditInfo(Some(now), Some(now))
    }
  }
}
