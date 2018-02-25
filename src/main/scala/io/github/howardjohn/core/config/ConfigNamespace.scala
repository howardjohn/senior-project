package io.github.howardjohn.core.config

import io.github.howardjohn.core.{Result, TagEntry, VersionEntry}

trait ConfigNamespace {
  def namespace: String

  def getVersions(): Result[Seq[VersionEntry]]
  def getTags(): Result[Seq[TagEntry]]

  def getVersion(version: String): ConfigVersion
  def getTag(tag: String): ConfigTag

  def createVersion(version: String): Result[ConfigVersion]
  def createTag(tag: String, version: String): Result[ConfigTag]
}
