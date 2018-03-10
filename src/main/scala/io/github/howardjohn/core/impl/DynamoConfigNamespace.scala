package io.github.howardjohn.core.impl

import cats.data.EitherT
import cats.implicits._
import com.gu.scanamo.syntax._
import io.github.howardjohn.core._
import io.github.howardjohn.core.config.{ConfigNamespace, ConfigTag, ConfigVersion}

class DynamoConfigNamespace(val namespace: String, scanamo: Scanamo) extends ConfigNamespace {

  def getVersion(version: String): ConfigVersion =
    new DynamoConfigVersion(namespace, version, scanamo)

  def getVersions(): Result[Seq[VersionEntry]] =
    EitherT(scanamo.execRead(Scanamo.versionsTable.query('namespace -> namespace)).value)

  def createVersion(version: String): Result[ConfigVersion] =
    scanamo
      .exec {
        Scanamo.versionsTable
          .given(attributeNotExists('namespace) and attributeNotExists('version))
          .put(VersionEntry(namespace, version, frozen = false, AuditInfo.default()))
      }
      .map(_ => getVersion(version))
}
