package io.github.howardjohn.config

trait ConfigNamespace[T] {
  def namespace: String

  def getTag(tag: String): ConfigTag
  def createTag(tag: String): Result[ConfigTag]

  def getVersions(): Result[Seq[VersionEntry]]
  def getVersion(version: String): ConfigVersion[T]

  def createVersion(version: String): Result[ConfigVersion[T]]
}
