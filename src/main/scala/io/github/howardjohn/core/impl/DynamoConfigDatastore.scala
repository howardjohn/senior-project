package io.github.howardjohn.core.impl

import cats.data.EitherT
import cats.effect.IO
import io.github.howardjohn.core._
import io.github.howardjohn.core.config.{ConfigDatastore, ConfigNamespace}

class DynamoConfigDatastore(scanamo: Scanamo) extends ConfigDatastore {

  def createNamespace(namespace: String): Result[ConfigNamespace] = EitherT {
    throw new RuntimeException("Not implemented")
  }

  def getNamespace(namespace: String): ConfigNamespace =
    new DynamoConfigNamespace(namespace, scanamo)
}

object DynamoConfigDatastore {}
