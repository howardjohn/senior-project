package io.github.howardjohn.core.impl

import cats.data.EitherT
import cats.effect.IO
import cats.implicits._
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsync
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException
import com.gu.scanamo.error.{DynamoReadError, TypeCoercionError}
import com.gu.scanamo.ops.ScanamoOps
import com.gu.scanamo.syntax._
import com.gu.scanamo.{DynamoFormat, ScanamoAsync, Table}
import io.circe.Json
import io.circe.parser.parse
import io.github.howardjohn.core.config.ConfigDatastore.{ConfigEntry, Result}
import io.github.howardjohn.core.config.ConfigError._
import io.github.howardjohn.core.config.{ConfigError, ConfigVersion}

import scala.concurrent.ExecutionContext.Implicits.global

class DynamoConfigVersion(val namespace: String, val version: String, dynamo: AmazonDynamoDBAsync)
    extends ConfigVersion {
  import DynamoConfigVersion._

  implicit val jsonFormat = DynamoFormat.xmap[Json, String](
    in => parse(in).left.map(f => TypeCoercionError(f))
  )(
    json => json.noSpaces
  )
  val versionsTable = Table[VersionEntry](versionsTableName)
  val table = Table[ConfigEntry](namespace)

  def cloneVersion(newVersionName: String): Result[ConfigVersion] =
    throw new RuntimeException("Not implemented yet")

  def write(key: String, value: Json): Result[Unit] =
    condExec {
      table.put(ConfigEntry(key, version, value))
    }.map(_.right.map(_ => ()))

  def get(key: String): Result[Option[ConfigEntry]] =
    exec(table.get('key -> key and 'version -> version))
      .map(_.sequence)
      .map(o => o.left.map(e => ReadError(DynamoReadError.describe(e))))

  def getAll(): Result[List[ConfigEntry]] =
    exec(table.index(versionIndex).query('version -> version))
      .map(_.sequence)
      .map(o => o.left.map(e => ReadError(DynamoReadError.describe(e))))

  def delete(key: String): Result[Unit] =
    condExec {
      table
        .delete('key -> key and 'version -> version)
    }.map(e => e.right.map(_ => ()))

  private def exec[A](ops: ScanamoOps[A]): IO[A] = IO.fromFuture(IO(ScanamoAsync.exec(dynamo)(ops)))

  private def condExec[A](ops: ScanamoOps[A]): Result[A] =
    isFrozen()
      .map(o => o.fold[Either[ConfigError, Boolean]](Left(ReadError("")))(Right(_)))

      .flatMap {
        case Right(true) => IO(Left(FrozenVersion))
        case Right(false) => exec(ops).map(Right(_))
        case Left(e) => IO(Left(e))
      }

  private def isFrozen(): IO[Option[Boolean]] =
    exec {
      versionsTable.get('namespace -> namespace and 'version -> version)
    }.map(oe => EitherT(oe).fold(_ => None, x => Some(x)).flatten)
      .map(_.map(_.frozen))
}

object DynamoConfigVersion {

  val versionsTableName = "Versions"
  val versionIndex = "version-index"

  case class VersionEntry(
    namespace: String,
    version: String,
    frozen: Boolean
  )

  def mapErrors[A, E](result: Either[E, A]): Either[ConfigError, A] = result.left.map {
    case _: ConditionalCheckFailedException => IllegalWrite
    case e => UnknownError(e)
  }
}
