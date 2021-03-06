package io.github.howardjohn.config.backend.impl

import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType._
import io.github.howardjohn.config.test.ConfigBehavior
import org.scalatest.{FeatureSpec, GivenWhenThen}

import scala.collection.JavaConverters._

class ConfigSpec extends FeatureSpec with GivenWhenThen with ConfigBehavior {
  def makeDatastore() = {
    val dynamo = LocalDynamoDB.client()
    dynamo.listTables().getTableNames.asScala.foreach(dynamo.deleteTable)

    AwsDynamoDb
      .createTableWithIndex(dynamo)(ConfigBehavior.namespace, "version-index")('key -> S, 'version -> S)('version -> S)
      .get
    AwsDynamoDb.createTable(dynamo)("Tags")('tag -> S, 'namespace -> S).get
    AwsDynamoDb.createTable(dynamo)("Versions")('namespace -> S, 'version -> S).get

    implicit val ec = scala.concurrent.ExecutionContext.Implicits.global
    new DynamoConfigDatastore(new Scanamo(dynamo))
  }

  scenariosFor(configDatastore(makeDatastore()))
}
