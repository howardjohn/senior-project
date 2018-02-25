package io.github.howardjohn.core.config

import io.github.howardjohn.core.Result

trait ConfigDatastore {
  def createNamespace(namespace: String): Result[ConfigNamespace]
  def getNamespace(namespace: String): ConfigNamespace
}