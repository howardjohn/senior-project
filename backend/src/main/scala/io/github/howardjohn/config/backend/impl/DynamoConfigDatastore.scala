package io.github.howardjohn.config.backend.impl

import com.gu.scanamo.DynamoFormat
import io.github.howardjohn.config._

class DynamoConfigDatastore(scanamo: Scanamo) extends ConfigDatastore[DynamoFormat] {
  def getNamespace[T: DynamoFormat](namespace: String): ConfigNamespace[T] =
    new DynamoConfigNamespace[T](namespace, scanamo)

  def createNamespace[T: DynamoFormat](namespace: String): Result[ConfigNamespace[T]] = ???
}
