package io.github.howardjohn.core.impl

import cats.effect.IO
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsync
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException
import com.gu.scanamo.error.TypeCoercionError
import com.gu.scanamo.{DynamoFormat, ScanamoAsync, Table}
import com.gu.scanamo.ops.ScanamoOps
import io.circe.Json
import io.circe.parser.parse
import io.github.howardjohn.core.config.ConfigDatastore.ConfigEntry
import io.github.howardjohn.core.config.ConfigError
import io.github.howardjohn.core.config.ConfigError.{IllegalWrite, UnknownError}
import io.github.howardjohn.core.config.ConfigVersion.VersionEntry

import scala.concurrent.ExecutionContext

class Scanamo(val dynamo: AmazonDynamoDBAsync)(implicit ec: ExecutionContext) {
  def exec[A](ops: ScanamoOps[A]): IO[A] = IO.fromFuture(IO(ScanamoAsync.exec(dynamo)(ops)))
}

object Scanamo {
  val versionsTableName = "Versions"
  val versionsTable: Table[VersionEntry] = Table[VersionEntry](versionsTableName)

  implicit val jsonFormat: DynamoFormat[Json] = DynamoFormat.xmap[Json, String](
    in => parse(in).left.map(f => TypeCoercionError(f))
  )(
    json => json.noSpaces
  )
  def configTable(namespace: String): Table[ConfigEntry] = Table[ConfigEntry](namespace)

  def mapErrors[A, E](result: Either[E, A]): Either[ConfigError, A] = result.left.map {
    case _: ConditionalCheckFailedException => IllegalWrite
    case e => UnknownError(e.toString)
  }
}
