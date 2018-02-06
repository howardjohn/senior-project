package io.github.howardjohn.core

import cats.effect.IO
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.gu.scanamo._
import com.gu.scanamo.error.{DynamoReadError, TypeCoercionError}
import com.gu.scanamo.ops.ScanamoOps
import com.gu.scanamo.syntax._
import io.circe.Json
import io.circe.syntax._
import io.circe.parser._
import io.github.howardjohn.core.ConfigDatastore.ConfigEntry

class DynamoConfigDatastore(dynamo: AmazonDynamoDB, table: Table[ConfigEntry]) extends ConfigDatastore {
  implicit val format = DynamoConfigDatastore.jsonFormat
  private def exec[A](ops: ScanamoOps[A]): A = Scanamo.exec(dynamo)(ops)

  def write(key: String, value: Json): IO[Unit] = IO {
    exec {
      table
        .given(attributeNotExists('key))
        .put(ConfigEntry(key, value))
    }
  }

  def update(key: String, value: Json): IO[ConfigEntry] = IO {
    exec {
      table
        .update('key -> key, set('value -> value))
        .map(either => either.right.get)
    }
  }

  def get(key: String): IO[Option[ConfigEntry]] = IO {
    exec(table.get('key -> key))
      .map(either => either.right.get)
  }

  def getAll(): IO[Seq[ConfigEntry]] = IO {
    exec(table.scan()).collect {
      case Right(entry) => entry
    }
  }

  def delete(key: String): IO[Unit] = IO {
    exec(table.delete('key -> key))
  }
}

object DynamoConfigDatastore {
  implicit val jsonFormat = DynamoFormat.xmap[Json, String](
    in => parse(in).left.map(f => TypeCoercionError(f))
  )(
    json => json.noSpaces
  )
}
