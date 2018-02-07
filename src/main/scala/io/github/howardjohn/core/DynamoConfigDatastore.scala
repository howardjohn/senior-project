package io.github.howardjohn.core

import cats.effect.IO
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.gu.scanamo._
import com.gu.scanamo.error.TypeCoercionError
import com.gu.scanamo.ops.ScanamoOps
import com.gu.scanamo.syntax._
import io.circe.Json
import io.circe.parser._
import io.github.howardjohn.core.ConfigDatastore.ConfigEntry

class DynamoConfigDatastore(dynamo: AmazonDynamoDB) extends ConfigDatastore {
  implicit val format = DynamoConfigDatastore.jsonFormat
  private def exec[A](ops: ScanamoOps[A]): A = Scanamo.exec(dynamo)(ops)
  private def table(namespace: String) = Table[ConfigEntry](namespace)

  def write(namespace: String)(key: String, value: Json): IO[Unit] = IO {
    exec {
      table(namespace)
        .given(attributeNotExists('key))
        .put(ConfigEntry(key, value))
    }
  }

  def update(namespace: String)(key: String, value: Json): IO[ConfigEntry] = IO {
    exec {
      table(namespace)
        .update('key -> key, set('value -> value))
        .map(either => either.right.get)
    }
  }

  def get(namespace: String)(key: String): IO[Option[ConfigEntry]] = IO {
    exec(table(namespace).get('key -> key))
      .map(either => either.right.get)
  }

  def getAll(namespace: String): IO[Seq[ConfigEntry]] = IO {
    exec(table(namespace).scan()).collect {
      case Right(entry) => entry
    }
  }

  def delete(namespace: String)(key: String): IO[Unit] = IO {
    exec(table(namespace).delete('key -> key))
  }

  def createNamespace(namespace: String): IO[Unit] = IO {
    throw new RuntimeException("Not implemented")
  }
}

object DynamoConfigDatastore {
  implicit val jsonFormat = DynamoFormat.xmap[Json, String](
    in => parse(in).left.map(f => TypeCoercionError(f))
  )(
    json => json.noSpaces
  )
}
