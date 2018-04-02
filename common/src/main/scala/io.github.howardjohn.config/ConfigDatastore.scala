package io.github.howardjohn.config

trait ConfigDatastore[B[_]] {
  def getNamespace[T: B](namespace: String): ConfigNamespace[T]

  def createNamespace[T: B](namespace: String): Result[ConfigNamespace[T]]
}
