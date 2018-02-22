package io.github.howardjohn.core.impl

import java.time.Instant

import cats.data.EitherT
import cats.effect.IO
import cats.implicits._
import com.gu.scanamo.error.DynamoReadError
import com.gu.scanamo.ops.ScanamoOps
import com.gu.scanamo.syntax._
import io.circe.Json
import io.github.howardjohn.core.config.ConfigDatastore.{ConfigEntry, Result}
import io.github.howardjohn.core.config.ConfigError._
import io.github.howardjohn.core.config.ConfigVersion.VersionEntry
import io.github.howardjohn.core.config.{ConfigError, ConfigVersion}

class DynamoConfigVersion(val namespace: String, val version: String, scanamo: Scanamo) extends ConfigVersion {
  import DynamoConfigVersion._

  val table = Scanamo.configTable(namespace)
  private def now = Instant.now.toEpochMilli

  def cloneVersion(newVersionName: String): Result[ConfigVersion] =
    throw new RuntimeException("Not implemented yet")

  def details(): Result[Option[VersionEntry]] =
    scanamo
      .exec(Scanamo.versionsTable.get('namespace -> namespace and 'version -> version))
      .map(_.sequence)
      .map(o => o.left.map(e => ReadError(DynamoReadError.describe(e))))

  def freeze(): Result[Unit] =
    scanamo
      .exec(
        Scanamo.versionsTable.update(
          'namespace -> namespace and 'version -> version,
          set('frozen -> true) and set('auditInfo \ 'modifiedTime -> now)))
      .map(Scanamo.mapErrors)
      .map(_.right.map(_ => ()))

  def write(key: String, value: Json): Result[Unit] =
    condExec {
      table.put(ConfigEntry(key, version, value))
    }.map(_.right.map(_ => ()))

  def get(key: String): Result[Option[ConfigEntry]] =
    scanamo
      .exec(table.get('key -> key and 'version -> version))
      .map(_.sequence)
      .map(o => o.left.map(e => ReadError(DynamoReadError.describe(e))))

  def getAll(): Result[List[ConfigEntry]] =
    scanamo
      .exec(table.index(versionIndex).query('version -> version))
      .map(_.sequence)
      .map(o => o.left.map(e => ReadError(DynamoReadError.describe(e))))

  def delete(key: String): Result[Unit] =
    condExec {
      table
        .delete('key -> key and 'version -> version)
    }.map(e => e.right.map(_ => ()))

  private def condExec[A](ops: ScanamoOps[A]): Result[A] =
    isFrozen()
      .map(o => o.fold[Either[ConfigError, Boolean]](Left(ReadError("")))(Right(_)))
      .flatMap {
        case Right(true) => IO(Left(FrozenVersion))
        case Right(false) => scanamo.exec(ops).map(Right(_))
        case Left(e) => IO(Left(e))
      }

  private def isFrozen(): IO[Option[Boolean]] =
    scanamo
      .exec {
        Scanamo.versionsTable.get('namespace -> namespace and 'version -> version)
      }
      .map(oe => EitherT(oe).fold(_ => None, x => Some(x)).flatten)
      .map(_.map(_.frozen))
}

object DynamoConfigVersion {
  val versionIndex = "version-index"
}
