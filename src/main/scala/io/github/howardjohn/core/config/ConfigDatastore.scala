package io.github.howardjohn.core.config

import io.github.howardjohn.core.Result

trait ConfigDatastore {
  def getNamespace(namespace: String): ConfigNamespace

  def createNamespace(namespace: String): Result[ConfigNamespace]
}