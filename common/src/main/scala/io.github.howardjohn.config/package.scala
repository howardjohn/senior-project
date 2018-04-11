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

    /**
     * @param getNow a function that gets the current time.
     * @return an audit referring to the current time.
     */
    def default(getNow: => Long = Instant.now.toEpochMilli): AuditInfo = {
      val now = getNow
      AuditInfo(Some(now), Some(now))
    }
  }

  sealed trait ConfigError
  object ConfigError {

    /**
     * This indicates that a record could not be found.
     */
    case object NotFound extends ConfigError

    /**
     * A write operation was attempted that was not allowed. This is likely caused by trying to mutate a frozen version.
     */
    case class IllegalWrite(cause: String) extends ConfigError

    /**
     * A field was required, but not provided.
     */
    case class MissingField(cause: String) extends ConfigError

    /**
     * This encapsulates any errors related to reading requests or responses, typically from network issues or bad data.
     */
    case class ReadError(cause: String) extends ConfigError

    /**
     * Any unknown error.
     */
    case class UnknownError(cause: String) extends ConfigError
  }

  /**
   * Translate an optional result into a result. If there was no result, a NotFound error is returned instead.
   * @param item to translate
   * @tparam A the type of the item
   * @return the item, or a NotFound error.
   */
  def orNotFound[A](item: Result[Option[A]]): Result[A] = EitherT {
    item.value.map {
      case Right(Some(value)) => Right(value)
      case Right(None) => Left(ConfigError.NotFound)
      case Left(error) => Left(error)
    }
  }
}
