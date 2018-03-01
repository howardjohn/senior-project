package io.github.howardjohn.core.impl

import cats.Traverse
import cats.effect.IO
import cats.implicits._
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsync
import com.amazonaws.services.dynamodbv2.document.Item
import com.amazonaws.services.dynamodbv2.document.internal.InternalUtils
import com.amazonaws.services.dynamodbv2.model.{AttributeValue, ConditionalCheckFailedException}
import com.gu.scanamo.error.{DynamoReadError, TypeCoercionError}
import com.gu.scanamo.ops.ScanamoOps
import com.gu.scanamo.{DynamoFormat, ScanamoAsync, Table}
import io.circe.Json
import io.circe.parser.parse
import io.github.howardjohn.core.ConfigError._
import io.github.howardjohn.core._

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext

class Scanamo(val dynamo: AmazonDynamoDBAsync)(implicit ec: ExecutionContext) {
  def exec[A](ops: ScanamoOps[A]): IO[A] = IO.fromFuture(IO(ScanamoAsync.exec(dynamo)(ops)))

  def execRead[A, F[_]: Traverse](ops: ScanamoOps[F[Either[DynamoReadError, A]]]): Result[F[A]] =
    exec {
      ops
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

  implicit val jsonFormat: DynamoFormat[Json] = new DynamoFormat[Json] {
    private val placeholder = "document"

    def read(av: AttributeValue): Either[DynamoReadError, Json] =
      parse {
        InternalUtils
          .toItemList(List(Map(placeholder -> av).asJava).asJava)
          .asScala
          .head
          .getJSON(placeholder)
      }.left.map(f => TypeCoercionError(f))

    def write(json: Json): AttributeValue = {
      val item = new Item().withJSON(placeholder, json.noSpaces)
      InternalUtils.toAttributeValues(item).get(placeholder)
    }
  }

  def mapErrors[A, E](result: Either[E, A]): Either[ConfigError, A] = result.left.map {
    case _: ConditionalCheckFailedException => IllegalWrite
    case e => UnknownError(e.toString)
  }
}
