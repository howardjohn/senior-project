package io.github.howardjohn.config.backend.impl

import java.time.Instant

import cats.data.EitherT
import cats.effect.IO
import cats.implicits._
import com.gu.scanamo.DynamoFormat
import com.gu.scanamo.ops.ScanamoOps
import com.gu.scanamo.syntax._
import io.github.howardjohn.config._
import io.github.howardjohn.config.ConfigError._

class DynamoConfigVersion[T: DynamoFormat](val namespace: String, val version: String, scanamo: Scanamo) extends ConfigVersion[T] {
  import DynamoConfigVersion._

  val table = Scanamo.configTable[T](namespace)
  private def now = Instant.now.toEpochMilli

  def cloneVersion(newVersionName: String): Result[ConfigVersion[T]] = ???

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

  def write(key: String, value: T): Result[Unit] =
    condExec {
      table.put(ConfigEntry[T](key, version, value, AuditInfo.default()))
    }.map(x => x.sequence)
      .leftMap(Scanamo.mapErrors)
      .map(_ => ())

  def get(key: String): Result[Option[ConfigEntry[T]]] =
    scanamo.execRead(table.get('key -> key and 'version -> version))

  def getAll(): Result[Seq[ConfigEntry[T]]] =
    EitherT(scanamo.execRead(table.index(versionIndex).query('version -> version)).value)

  def delete(key: String): Result[Unit] =
    condExec {
      table
        .delete('key -> key and 'version -> version)
    }.map(_ => ())

  private def condExec[A](ops: ScanamoOps[A]): Result[A] =
    isFrozen()
      .map(e => e.map(Right(_)).getOrElse(Left(ReadError("Could not determine if the tag was frozen"))))
      .transform(_.joinRight)
      .flatMap {
        case true => EitherT.fromEither[IO](Left(IllegalWrite("Cannot write to a frozen version.")))
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
