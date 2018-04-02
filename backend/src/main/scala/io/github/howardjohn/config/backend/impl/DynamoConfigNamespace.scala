package io.github.howardjohn.config.backend.impl

import cats.data.EitherT
import cats.implicits._
import com.gu.scanamo.DynamoFormat
import com.gu.scanamo.syntax._
import io.github.howardjohn.config._

class DynamoConfigNamespace[T: DynamoFormat](val namespace: String, scanamo: Scanamo) extends ConfigNamespace[T] {
  def getTag(tag: String): ConfigTag =
    new DynamoConfigTag(tag, namespace, scanamo)

  def createTag(tag: String): Result[ConfigTag] =
    scanamo
      .execRead {
        Scanamo.tagsTable
          .put(DynamoTagEntry(tag, namespace, Map(), AuditInfo.default()))
      }
      .map(_ => getTag(tag))

  def getVersion(version: String): ConfigVersion[T] =
    new DynamoConfigVersion[T](namespace, version, scanamo)

  def getVersions(): Result[Seq[VersionEntry]] =
    EitherT(scanamo.execRead(Scanamo.versionsTable.query('namespace -> namespace)).value)

  def createVersion(version: String): Result[ConfigVersion[T]] =
    scanamo
      .exec {
        Scanamo.versionsTable
          .given(attributeNotExists('namespace) and attributeNotExists('version))
          .put(VersionEntry(namespace, version, frozen = false, AuditInfo.default()))
      }
      .map(_ => getVersion(version))
}
