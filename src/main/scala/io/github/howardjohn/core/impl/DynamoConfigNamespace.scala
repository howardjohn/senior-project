package io.github.howardjohn.core.impl

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsync
import io.github.howardjohn.core.config.ConfigDatastore.Result
import io.github.howardjohn.core.config.{ConfigNamespace, ConfigVersion}

class DynamoConfigNamespace(val namespace: String, dynamo: AmazonDynamoDBAsync) extends ConfigNamespace {
  def getVersion(version: String): ConfigVersion =
    new DynamoConfigVersion(namespace, version, dynamo)

  def createVersion(name: String): Result[ConfigVersion] =
    throw new RuntimeException("Not yet implemented")
}
