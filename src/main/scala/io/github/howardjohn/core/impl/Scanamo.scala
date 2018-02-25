package io.github.howardjohn.core.impl

import cats.effect.IO
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsync
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException
import com.gu.scanamo.error.{DynamoReadError, TypeCoercionError}
import com.gu.scanamo.ops.ScanamoOps
import com.gu.scanamo.query.Query
import com.gu.scanamo.{DynamoFormat, ScanamoAsync, SecondaryIndex, Table}
import io.circe.Json
import io.circe.parser.parse
import io.github.howardjohn.core.ConfigError._
import io.github.howardjohn.core._

import scala.concurrent.ExecutionContext
import cats.implicits._

class Scanamo(val dynamo: AmazonDynamoDBAsync)(implicit ec: ExecutionContext) {
  def exec[A](ops: ScanamoOps[A]): IO[A] = IO.fromFuture(IO(ScanamoAsync.exec(dynamo)(ops)))

  def query[A](table: Table[A])(query: Query[_]): Result[Seq[A]] =
    exec {
      table
        .query(query)
        .map(_.sequence)
        .map(o => o.left.map(e => ReadError(DynamoReadError.describe(e))))
    }

  def query[A](index: SecondaryIndex[A])(query: Query[_]): Result[Seq[A]] =
    exec {
      index
        .query(query)
        .map(_.sequence)
        .map(o => o.left.map(e => ReadError(DynamoReadError.describe(e))))
    }
}

object Scanamo {
  val versionsTableName = "Versions"
  val versionsTable: Table[VersionEntry] = Table[VersionEntry](versionsTableName)
  val tagsTableName = "Tags"
  val tagsTable: Table[TagEntry] = Table[TagEntry](tagsTableName)

  def configTable(namespace: String): Table[ConfigEntry] = Table[ConfigEntry](namespace)

  implicit val jsonFormat: DynamoFormat[Json] = DynamoFormat.xmap[Json, String](
    in => parse(in).left.map(f => TypeCoercionError(f))
  )(
    json => json.noSpaces
  )

  def mapErrors[A, E](result: Either[E, A]): Either[ConfigError, A] = result.left.map {
    case _: ConditionalCheckFailedException => IllegalWrite
    case e => UnknownError(e.toString)
  }
}
