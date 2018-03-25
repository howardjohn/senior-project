package io.github.howardjohn.config

trait ConfigNamespace[T] {
  def namespace: String

  def getVersions(): Result[Seq[VersionEntry]]

  def getVersion(version: String): ConfigVersion[T]

  def createVersion(version: String): Result[ConfigVersion[T]]
}
