package io.github.howardjohn.core.impl

import cats.effect.IO
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsync
import io.github.howardjohn.core.config.{ConfigDatastore, ConfigNamespace}
import io.github.howardjohn.core.config.ConfigDatastore.Result

class DynamoConfigDatastore(dynamo: AmazonDynamoDBAsync) extends ConfigDatastore {

  def createNamespace(namespace: String): Result[ConfigNamespace] = IO {
    throw new RuntimeException("Not implemented")
  }

  def getNamespace(namespace: String): ConfigNamespace =
    new DynamoConfigNamespace (namespace, dynamo)
}

object DynamoConfigDatastore {}
