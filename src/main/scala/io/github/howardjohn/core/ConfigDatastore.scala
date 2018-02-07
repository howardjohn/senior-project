package io.github.howardjohn.core

import cats.effect.IO
import io.circe.Json
import io.github.howardjohn.core.ConfigDatastore.ConfigEntry

trait ConfigDatastore {
  def write(namespace: String)(key: String, value: Json): IO[Unit]
  def update(namespace: String)(key: String, value: Json): IO[ConfigEntry]
  def get(namespace: String)(key: String): IO[Option[ConfigEntry]]
  def getAll(namespace: String): IO[Seq[ConfigEntry]]
  def delete(namespace: String)(key: String): IO[Unit]
  def createNamespace(namespace: String): IO[Unit]
}

object ConfigDatastore {
  case class ConfigEntry(
    key: String,
    value: Json
  )
}
