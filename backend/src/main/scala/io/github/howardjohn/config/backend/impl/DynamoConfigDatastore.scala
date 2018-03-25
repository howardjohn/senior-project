package io.github.howardjohn.config.backend.impl

import cats.data.EitherT
import cats.implicits._
import com.gu.scanamo.DynamoFormat
import io.github.howardjohn.config._

class DynamoConfigDatastore(scanamo: Scanamo) extends ConfigDatastore[DynamoFormat] {

  def getNamespace[T: DynamoFormat](namespace: String): ConfigNamespace[T] =
    new DynamoConfigNamespace[T](namespace, scanamo)

  def getTag(tag: String): ConfigTag =
    new DynamoConfigTag(tag, scanamo)

  def createNamespace[T: DynamoFormat](namespace: String): Result[ConfigNamespace[T]] = EitherT {
    throw new RuntimeException("Not implemented")
  }

  def createTag(tag: String, namespace: String): Result[ConfigTag] =
    scanamo
      .execRead {
        Scanamo.tagsTable
          .put(DynamoTagEntry(tag, namespace, Map(), AuditInfo.default()))
      }
      .map(_ => getTag(tag))
}

object DynamoConfigDatastore {}
