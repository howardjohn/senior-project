package io.github.howardjohn.core.impl

import cats.data.EitherT
import cats.implicits._
import io.github.howardjohn.core._
import io.github.howardjohn.core.config.{ConfigDatastore, ConfigNamespace, ConfigTag}

class DynamoConfigDatastore(scanamo: Scanamo) extends ConfigDatastore {

  def getNamespace(namespace: String): ConfigNamespace =
    new DynamoConfigNamespace(namespace, scanamo)

  def getTag(tag: String): ConfigTag =
    new DynamoConfigTag(tag, scanamo)

  def createNamespace(namespace: String): Result[ConfigNamespace] = EitherT {
    throw new RuntimeException("Not implemented")
  }

  def createTag(tag: String, namespace: String, version: String): Result[ConfigTag] =
    scanamo
      .execRead {
        Scanamo.tagsTable
          .put(DynamoTagEntry(tag, namespace, Seq(DynamoTagEntryVersion(1, version)), AuditInfo.default()))
      }
      .map(_ => getTag(tag))
}

object DynamoConfigDatastore {}
