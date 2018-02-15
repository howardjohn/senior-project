package io.github.howardjohn.core.config

import io.github.howardjohn.core.config.ConfigDatastore.Result

trait ConfigNamespace {
  def namespace: String

  def getVersion(version: String): ConfigVersion
  def createVersion(version: String): Result[ConfigVersion]
}
