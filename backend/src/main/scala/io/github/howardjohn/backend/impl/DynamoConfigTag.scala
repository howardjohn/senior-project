package io.github.howardjohn.backend.impl

import java.security.MessageDigest
import java.time.Instant

import cats.implicits._
import com.gu.scanamo.syntax._
import io.github.howardjohn.backend.ConfigError.MissingDiscriminator
import io.github.howardjohn.backend.{ConfigError, Result, TagEntry}
import io.github.howardjohn.backend.config.ConfigTag

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

  def moveTag(namespace: String, versions: Map[String, Int]): Result[Unit] =
    scanamo
      .exec {
        Scanamo.tagsTable.update(
          'tag -> tagName and 'namespace -> namespace,
          versions.toSeq.foldLeft(set('auditInfo \ 'modifiedTime -> now)) {
            case (ops, (ver, weight)) =>
              if (weight > 0) {
                ops and set('versions \ Symbol(ver) -> weight)
              } else {
                ops and remove('versions \ Symbol(ver))
              }
          }
        )
      }
      .map(_ => ())
}

object DynamoConfigTag {
  def md5(value: String) = MessageDigest.getInstance("MD5").digest(value.getBytes)

  def getVersion(versions: Map[String, Int], hash: Array[Byte]): Option[String] = {
    val totalWeight = versions.values.sum
    if (totalWeight == 0) {
      None
    } else {
      val bucket = BigInt(hash).abs % totalWeight

      def select(curVersions: Seq[(String, Int)], current: Int = 0): Option[String] = curVersions match {
        case hd :: tl => {
          val newSum = current + hd._2
          if (newSum > bucket) {
            Some(hd._1)
          } else {
            select(tl, newSum)
          }
        }
        case Nil => None
      }
      select(versions.toList)
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
    if (entry.versions.size <= 1) {
      Right {
        entry.versions.headOption.flatMap { v =>
          if (v._2 > 0) {
            Some(
              TagEntry(
                entry.tag,
                entry.namespace,
                v._1,
                entry.auditInfo
              ))
          } else {
            None
          }
        }
      }
    } else {
      Left(MissingDiscriminator)
    }
}
