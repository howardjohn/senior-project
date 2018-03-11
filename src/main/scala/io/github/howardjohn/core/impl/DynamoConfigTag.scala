package io.github.howardjohn.core.impl

import java.time.Instant

import cats.implicits._
import com.gu.scanamo.error.DynamoReadError
import com.gu.scanamo.syntax._
import io.github.howardjohn.core.ConfigError.MissingDiscriminator
import io.github.howardjohn.core.{ConfigError, Result, TagEntry}
import io.github.howardjohn.core.config.ConfigTag

class DynamoConfigTag(val tagName: String, scanamo: Scanamo) extends ConfigTag {
  import DynamoConfigTag._

  private def now = Instant.now.toEpochMilli

  def getDetails(namespace: String): Result[Option[TagEntry]] =
    scanamo
      .execRead {
        Scanamo.tagsTable
          .get('tag -> tagName and 'namespace -> namespace)
      }
      .subflatMap {
        case Some(te) => asTagEntry(te)
        case None => Right(None)
      }

  def getDetails(namespace: String, discriminator: String): Result[Option[TagEntry]] =
    scanamo
      .execRead {
        Scanamo.tagsTable
          .get('tag -> tagName and 'namespace -> namespace)
      }
      .map(_.flatMap(entry => asTagEntry(entry, discriminator)))

  def moveTag(namespace: String, version: String): Result[Unit] =
    scanamo
      .exec {
        Scanamo.tagsTable.update(
          'tag -> tagName and 'namespace -> namespace,
          set('version -> version) and set('auditInfo \ 'modifiedTime -> now))
      }
      .map(_ => ())
}

object DynamoConfigTag {
  def asTagEntry(entry: DynamoTagEntry, discriminator: String): Option[TagEntry] = {
    entry.versions.headOption.map {
      ver =>
        TagEntry(
          entry.tag,
          entry.namespace,
          ver.version,
          entry.auditInfo
        )
    }
  }

  def asTagEntry(entry: DynamoTagEntry): Either[ConfigError, Option[TagEntry]] =
    if (entry.versions.length <= 1) {
      Right {
        entry.versions.headOption.map { v =>
          TagEntry(
            entry.tag,
            entry.namespace,
            v.version,
            entry.auditInfo
          )
        }
      }
    } else {
      Left(MissingDiscriminator)
    }
}
