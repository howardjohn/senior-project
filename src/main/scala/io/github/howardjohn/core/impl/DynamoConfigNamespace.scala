package io.github.howardjohn.core.impl

import cats.implicits._
import com.gu.scanamo.error.DynamoReadError
import com.gu.scanamo.syntax._
import io.github.howardjohn.core.ConfigError._
import io.github.howardjohn.core._
import io.github.howardjohn.core.config.{ConfigNamespace, ConfigVersion}

class DynamoConfigNamespace(val namespace: String, scanamo: Scanamo) extends ConfigNamespace {

  def getVersion(version: String): ConfigVersion =
    new DynamoConfigVersion(namespace, version, scanamo)

  def getVersions(): Result[Seq[VersionEntry]] =
    scanamo.exec {
      Scanamo.versionsTable
        .query('namespace -> namespace)
        .map(_.sequence)
        .map(o => o.left.map(e => ReadError(DynamoReadError.describe(e))))
    }

  def createVersion(version: String): Result[ConfigVersion] =
    scanamo
      .exec {
        Scanamo.versionsTable
          .given(attributeNotExists('namespace) and attributeNotExists('version))
          .put(VersionEntry(namespace, version, frozen = false, AuditInfo.default()))
      }
      .map(Scanamo.mapErrors)
      .map(_.right.map(_ => getVersion(version)))
}
