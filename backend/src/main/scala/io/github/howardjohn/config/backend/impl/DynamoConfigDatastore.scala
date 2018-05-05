package io.github.howardjohn.config.backend.impl

import cats.data.EitherT
import cats.effect.IO
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType.S
import com.gu.scanamo.DynamoFormat
import io.github.howardjohn.config.ConfigError.IllegalWrite
import io.github.howardjohn.config._

import scala.collection.JavaConverters._

class DynamoConfigDatastore(scanamo: Scanamo) extends ConfigDatastore[DynamoFormat] {
  import DynamoConfigDatastore._

  def getNamespace[T: DynamoFormat](namespace: String): ConfigNamespace[T] =
    new DynamoConfigNamespace[T](namespace, scanamo)

  def getAllNamespaces(): Result[Seq[String]] = EitherT.liftF {
    scanamo
      .execAwsClient(_.listTablesAsync())
      .map(_.getTableNames.asScala.filter(!reservedNamespaces.contains(_)))
  }

  def createNamespace[T: DynamoFormat](namespace: String): Result[ConfigNamespace[T]] =
    if (!reservedNamespaces.contains(namespace)) {
      EitherT.liftF {
        scanamo
          .execAwsClient { dynamo =>
            AwsDynamoDb.createTableWithIndex(dynamo)(namespace, "version-index")('key -> S, 'version -> S)(
              'version -> S)
          }
          .map(_ => getNamespace(namespace))
      }
    } else {
      EitherT.left(IO(IllegalWrite(s"$namespace is a reserved namespace")))
    }
}

private object DynamoConfigDatastore {
  private val reservedNamespaces = Seq("Tags", "Versions")
}
