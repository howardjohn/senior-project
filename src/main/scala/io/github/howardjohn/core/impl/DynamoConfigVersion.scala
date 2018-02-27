package io.github.howardjohn.core.impl

import java.time.Instant

import cats.data.EitherT
import cats.effect.IO
import cats.implicits._
import com.gu.scanamo.error.DynamoReadError
import com.gu.scanamo.ops.ScanamoOps
import com.gu.scanamo.syntax._
import io.circe.Json
import io.github.howardjohn.core.ConfigError._
import io.github.howardjohn.core._
import io.github.howardjohn.core.config.ConfigVersion

class DynamoConfigVersion(val namespace: String, val version: String, scanamo: Scanamo) extends ConfigVersion {
  import DynamoConfigVersion._

  val table = Scanamo.configTable(namespace)
  private def now = Instant.now.toEpochMilli

  def cloneVersion(newVersionName: String): Result[ConfigVersion] =
    throw new RuntimeException("Not implemented yet")

  def details(): Result[Option[VersionEntry]] =
    scanamo.execRead(Scanamo.versionsTable.get('namespace -> namespace and 'version -> version))

  def freeze(): Result[Unit] =
    scanamo
      .exec {
        Scanamo.versionsTable.update(
          'namespace -> namespace and 'version -> version,
          set('frozen -> true) and set('auditInfo \ 'modifiedTime -> now))
      }
      .map(Scanamo.mapErrors)
      .map(_.map(_ => ()))

  def write(key: String, value: Json): Result[Unit] =
    condExec {
      table.put(ConfigEntry(key, version, value, AuditInfo.default()))
    }.map(_.map(_ => ()))

  def get(key: String): Result[Option[ConfigEntry]] =
    scanamo.execRead(table.get('key -> key and 'version -> version))

  def getAll(): Result[Seq[ConfigEntry]] =
    scanamo.execRead[ConfigEntry, List](table.index(versionIndex).query('version -> version))

  def delete(key: String): Result[Unit] =
    condExec {
      table
        .delete('key -> key and 'version -> version)
    }.map(e => e.map(_ => ()))

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
