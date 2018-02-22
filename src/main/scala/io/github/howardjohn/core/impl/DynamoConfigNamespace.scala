package io.github.howardjohn.core.impl

import com.gu.scanamo.syntax._
import io.github.howardjohn.core.config.ConfigDatastore.{AuditInfo, Result}
import io.github.howardjohn.core.config.ConfigVersion.VersionEntry
import io.github.howardjohn.core.config.{ConfigNamespace, ConfigVersion}

class DynamoConfigNamespace(val namespace: String, scanamo: Scanamo) extends ConfigNamespace {

  def getVersion(version: String): ConfigVersion =
    new DynamoConfigVersion(namespace, version, scanamo)

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
