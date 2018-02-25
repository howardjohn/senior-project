package io.github.howardjohn.core.config

import io.github.howardjohn.core.{Result, VersionEntry}

trait ConfigNamespace {
  def namespace: String

  def getVersions(): Result[Seq[VersionEntry]]

  def getVersion(version: String): ConfigVersion
  def createVersion(version: String): Result[ConfigVersion]
}
