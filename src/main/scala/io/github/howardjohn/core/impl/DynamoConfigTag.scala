package io.github.howardjohn.core.impl

import java.security.MessageDigest
import java.time.Instant

import cats.implicits._
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
  def md5(value: String) = MessageDigest.getInstance("MD5").digest(value.getBytes)

  def getVersion(versions: Seq[DynamoTagEntryVersion], hash: Array[Byte]): Option[String] = {
    val totalWeight = versions.map(_.weight).sum
    if (totalWeight == 0) {
      None
    } else {
      val bucket = BigInt(hash) % totalWeight

      def select(curVersions: Seq[DynamoTagEntryVersion], current: Int = 0): Option[String] = curVersions match {
        case hd :: tl => {
          val newSum = current + hd.weight
          if (newSum > bucket) {
            Some(hd.version)
          } else {
            select(tl, newSum)
          }
        }
        case Nil => None
      }
      select(versions)
    }
  }

  def asTagEntry(entry: DynamoTagEntry, discriminator: String): Option[TagEntry] =
    getVersion(entry.versions, md5(discriminator)).map { ver =>
      TagEntry(
        entry.tag,
        entry.namespace,
        ver,
        entry.auditInfo
      )
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
