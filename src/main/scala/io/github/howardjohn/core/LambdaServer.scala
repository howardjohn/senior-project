package io.github.howardjohn.core

import com.amazonaws.regions.Regions
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import io.circe.generic.auto._
import io.github.howardjohn.http4s.lambda.LambdaHandler

object LambdaServer {
  val dynamo = AmazonDynamoDBClientBuilder
    .standard()
    .withRegion(Regions.US_WEST_2)
    .build()

  implicit val jsonFormt = DynamoConfigDatastore.jsonFormat
  val route = new Route(new DynamoConfigDatastore(dynamo))

  class EntryPoint extends LambdaHandler(route.service)
}
