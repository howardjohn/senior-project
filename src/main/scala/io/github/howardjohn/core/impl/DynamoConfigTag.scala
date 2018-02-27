package io.github.howardjohn.core.impl

import java.time.Instant

import cats.implicits._
import com.gu.scanamo.syntax._
import io.github.howardjohn.core.{Result, TagEntry}
import io.github.howardjohn.core.config.ConfigTag

class DynamoConfigTag(val namespace: String, val tagName: String, scanamo: Scanamo) extends ConfigTag {
  private def now = Instant.now.toEpochMilli

  def getDetails(): Result[Option[TagEntry]] =
    scanamo.execRead(Scanamo.tagsTable.get('namespace -> namespace and 'tag -> tagName))

  def moveTag(version: String): Result[Unit] =
    scanamo
      .exec {
        Scanamo.tagsTable.update(
          'namespace -> namespace and 'tag -> tagName,
          set('version -> version) and set('auditInfo \ 'modifiedTime -> now))
      }
      .map(Scanamo.mapErrors)
      .map(_.map(_ => ()))
}
