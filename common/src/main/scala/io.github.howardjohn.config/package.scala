package io.github.howardjohn

import java.time.Instant

import cats.data.EitherT
import cats.effect.IO

package object config {
  type Result[A] = EitherT[IO, ConfigError, A]

  case class ConfigEntry[T](
    key: String,
    version: String,
    value: T,
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

  object AuditInfo {
    def default(getNow: => Long = Instant.now.toEpochMilli): AuditInfo = {
      val now = getNow
      AuditInfo(Some(now), Some(now))
    }
  }

  sealed trait ConfigError
  object ConfigError {
    case object NotFound extends ConfigError
    case class IllegalWrite(cause: String) extends ConfigError
    case class MissingField(cause: String) extends ConfigError
    case class ReadError(cause: String) extends ConfigError
    case class UnknownError(cause: String) extends ConfigError
  }

  def orNotFound[A](item: Result[Option[A]]): Result[A] = EitherT {
    item.value.map {
      case Right(Some(value)) => Right(value)
      case Right(None) => Left(ConfigError.NotFound)
      case Left(error) => Left(error)
    }
  }
}
