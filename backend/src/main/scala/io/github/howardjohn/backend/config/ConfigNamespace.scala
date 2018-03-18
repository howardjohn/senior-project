package io.github.howardjohn.backend.config

import io.github.howardjohn.backend.{Result, VersionEntry}

trait ConfigNamespace {
  def namespace: String

  def getVersions(): Result[Seq[VersionEntry]]

  def getVersion(version: String): ConfigVersion

  def createVersion(version: String): Result[ConfigVersion]
}
