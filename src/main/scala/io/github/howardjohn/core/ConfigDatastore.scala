package io.github.howardjohn.core

import cats.effect.IO
import io.circe.Json
import io.github.howardjohn.core.ConfigDatastore.ConfigEntry

trait ConfigDatastore {
  def write(key: String, value: Json): IO[Unit]
  def update(key: String, value: Json): IO[ConfigEntry]
  def get(key: String): IO[Option[ConfigEntry]]
  def getAll(): IO[Seq[ConfigEntry]]
  def delete(key: String): IO[Unit]
}

object ConfigDatastore {
  case class ConfigEntry(
    key: String,
    value: Json
  )
}
