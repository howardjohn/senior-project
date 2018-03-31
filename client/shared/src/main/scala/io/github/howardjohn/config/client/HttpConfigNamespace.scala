package io.github.howardjohn.config.client

import io.github.howardjohn.config.{ConfigNamespace, ConfigVersion, Result, VersionEntry}

class HttpConfigNamespace[T](val namespace: String, http: HttpClient) extends ConfigNamespace[T] {
  def getVersions(): Result[Seq[VersionEntry]] = ???

  def getVersion(version: String): ConfigVersion[T] = ???

  def createVersion(version: String): Result[ConfigVersion[T]] = ???
}
