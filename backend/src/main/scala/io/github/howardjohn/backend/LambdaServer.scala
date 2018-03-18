package io.github.howardjohn.backend

import com.amazonaws.regions.Regions
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClientBuilder
import io.circe.generic.auto._
import io.github.howardjohn.backend.impl.{DynamoConfigDatastore, Scanamo}
import io.github.howardjohn.http4s.lambda.LambdaHandler

object LambdaServer {
  val dynamo = AmazonDynamoDBAsyncClientBuilder
    .standard()
    .withRegion(Regions.US_WEST_2)
    .build()

  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global
  val route = new Route(new DynamoConfigDatastore(new Scanamo(dynamo)))

  class EntryPoint extends LambdaHandler(route.service)
}
