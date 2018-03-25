package io.github.howardjohn.config.backend.impl

import cats.Traverse
import cats.data.EitherT
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
import io.github.howardjohn.config.{ConfigEntry, ConfigError, Result, VersionEntry}
import io.github.howardjohn.config.ConfigError._

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext

class Scanamo(val dynamo: AmazonDynamoDBAsync)(implicit ec: ExecutionContext) {
  import Scanamo._
  def execRaw[A](ops: ScanamoOps[A]): IO[A] = IO.fromFuture(IO(ScanamoAsync.exec(dynamo)(ops)))

  def exec[A, E](ops: ScanamoOps[Either[E, A]]): Result[A] =
    EitherT {
      execRaw(ops)
    }.leftMap(mapErrors)

  def execRead[A, F[_]: Traverse, E](ops: ScanamoOps[F[Either[E, A]]]): Result[F[A]] =
    EitherT {
      execRaw {
        ops
          .map(_.sequence)
      }
    }.leftMap(mapErrors)
}

object Scanamo {
  val versionsTableName = "Versions"
  val versionsTable: Table[VersionEntry] = Table[VersionEntry](versionsTableName)
  val tagsTableName = "Tags"
  val tagsTable: Table[DynamoTagEntry] = Table[DynamoTagEntry](tagsTableName)

  def configTable[T: DynamoFormat](namespace: String): Table[ConfigEntry[T]] = Table[ConfigEntry[T]](namespace)

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

  def mapErrors[E](error: E): ConfigError = error match {
    case e: DynamoReadError => ReadError(DynamoReadError.describe(e))
    case e: ConditionalCheckFailedException => IllegalWrite(e.getMessage)
    case e => UnknownError(e.toString)
  }
}
