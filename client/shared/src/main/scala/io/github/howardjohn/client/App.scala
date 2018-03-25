package io.github.howardjohn.client

trait ConfigDatastore {
  def getNamespace(namespace: String): ConfigNamespace
  def getTag(tag: String): ConfigTag

  def createNamespace(namespace: String): Result[ConfigNamespace]
  def createTag(tag: String, namespace: String): Result[ConfigTag]
}