package io.github.howardjohn.core.config


sealed trait ConfigError

object ConfigError {
  case object IllegalWrite extends ConfigError
  case object FrozenVersion extends ConfigError
  case class ReadError(cause: String) extends ConfigError
  case class UnknownError(cause: String) extends ConfigError
}
