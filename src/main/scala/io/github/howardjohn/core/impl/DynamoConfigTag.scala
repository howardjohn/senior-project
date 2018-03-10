package io.github.howardjohn.core.impl

import java.time.Instant

import cats.implicits._
import com.gu.scanamo.syntax._
import io.github.howardjohn.core.{Result, TagEntry}
import io.github.howardjohn.core.config.ConfigTag

class DynamoConfigTag(val tagName: String, scanamo: Scanamo) extends ConfigTag {
  private def now = Instant.now.toEpochMilli

  def getDetails(namespace: String): Result[Option[TagEntry]] =
    scanamo.execRead(Scanamo.tagsTable.get('tag -> tagName and 'namespace -> namespace))

  def moveTag(namespace: String, version: String): Result[Unit] =
    scanamo
      .exec {
        Scanamo.tagsTable.update(
          'tag -> tagName and 'namespace -> namespace,
          set('version -> version) and set('auditInfo \ 'modifiedTime -> now))
      }.map(_ => ())
}
