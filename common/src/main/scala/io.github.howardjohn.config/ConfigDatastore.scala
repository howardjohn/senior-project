package io.github.howardjohn.config

trait ConfigDatastore[B[_]] {
  def getNamespace[T: B](namespace: String): ConfigNamespace[T]
  def getTag(tag: String): ConfigTag

  def createNamespace[T: B](namespace: String): Result[ConfigNamespace[T]]
  def createTag(tag: String, namespace: String): Result[ConfigTag]
}
