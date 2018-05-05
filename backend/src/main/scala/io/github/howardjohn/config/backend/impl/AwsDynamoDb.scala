package io.github.howardjohn.config.backend.impl

import java.util.concurrent.Future

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsync
import com.amazonaws.services.dynamodbv2.model._

import scala.collection.JavaConverters._

object AwsDynamoDb {
  def createTable(client: AmazonDynamoDBAsync)(tableName: String)(
    attributes: (Symbol, ScalarAttributeType)*): Future[CreateTableResult] =
    client.createTableAsync(
      attributeDefinitions(attributes),
      tableName,
      keySchema(attributes),
      defaultThroughput
    )

  def createTableWithIndex(client: AmazonDynamoDBAsync)(tableName: String, secondaryIndexName: String)(
    primaryIndexAttributes: (Symbol, ScalarAttributeType)*)(
    secondaryIndexAttributes: (Symbol, ScalarAttributeType)*): Future[CreateTableResult] =
    client.createTableAsync(
      new CreateTableRequest()
        .withTableName(tableName)
        .withAttributeDefinitions(attributeDefinitions(
          primaryIndexAttributes.toList ++ (secondaryIndexAttributes.toList diff primaryIndexAttributes.toList)))
        .withKeySchema(keySchema(primaryIndexAttributes))
        .withProvisionedThroughput(defaultThroughput)
        .withGlobalSecondaryIndexes(
          new GlobalSecondaryIndex()
            .withIndexName(secondaryIndexName)
            .withKeySchema(keySchema(secondaryIndexAttributes))
            .withProvisionedThroughput(defaultThroughput)
            .withProjection(new Projection().withProjectionType(ProjectionType.ALL)))
    )

  private def keySchema(attributes: Seq[(Symbol, ScalarAttributeType)]) = {
    val hashKeyWithType :: rangeKeyWithType = attributes.toList
    val keySchemas = hashKeyWithType._1 -> KeyType.HASH :: rangeKeyWithType.map(_._1 -> KeyType.RANGE)
    keySchemas.map { case (symbol, keyType) => new KeySchemaElement(symbol.name, keyType) }.asJava
  }

  private def attributeDefinitions(attributes: Seq[(Symbol, ScalarAttributeType)]) =
    attributes.map { case (symbol, attributeType) => new AttributeDefinition(symbol.name, attributeType) }.asJava

  private val defaultThroughput = new ProvisionedThroughput(1L, 1L)
}
