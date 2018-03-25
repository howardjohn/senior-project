package io.github.howardjohn.config

trait ConfigVersion[T] {
  def version: String
  def namespace: String

  def details(): Result[Option[VersionEntry]]

  def freeze(): Result[Unit]
  def cloneVersion(newVersionName: String): Result[ConfigVersion[T]]

  def get(key: String): Result[Option[ConfigEntry[T]]]
  def getAll(): Result[Seq[ConfigEntry[T]]]
  def write(key: String, value: T): Result[Unit]
  def delete(key: String): Result[Unit]
}
