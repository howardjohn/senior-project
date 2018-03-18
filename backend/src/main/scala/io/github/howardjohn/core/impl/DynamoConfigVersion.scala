package io.github.howardjohn.core.impl

import java.time.Instant

import cats.data.EitherT
import cats.effect.IO
import cats.implicits._
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
      .map(_ => ())

  def write(key: String, value: Json): Result[Unit] =
    condExec {
      table.put(ConfigEntry(key, version, value, AuditInfo.default()))
    }.map(x => x.sequence)
      .leftMap(Scanamo.mapErrors)
      .map(_ => ())

  def get(key: String): Result[Option[ConfigEntry]] =
    scanamo.execRead(table.get('key -> key and 'version -> version))

  def getAll(): Result[Seq[ConfigEntry]] =
    EitherT(scanamo.execRead(table.index(versionIndex).query('version -> version)).value)

  def delete(key: String): Result[Unit] =
    condExec {
      table
        .delete('key -> key and 'version -> version)
    }.map(_ => ())

  private def condExec[A](ops: ScanamoOps[A]): Result[A] =
    isFrozen()
      .map(e => e.map(Right(_)).getOrElse(Left(ReadError("Cold not determine if the tag was frozen"))))
      .transform(_.joinRight)
      .flatMap {
        case true => EitherT.fromEither[IO](Left(FrozenVersion))
        case false => EitherT.liftF(scanamo.execRaw(ops))
      }

  private def isFrozen(): Result[Option[Boolean]] =
    scanamo
      .execRead {
        Scanamo.versionsTable.get('namespace -> namespace and 'version -> version)
      }
      .map(_.map(_.frozen))
}

object DynamoConfigVersion {
  val versionIndex = "version-index"
}
