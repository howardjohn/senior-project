package io.github.howardjohn.core

import cats.effect.IO
import com.amazonaws.regions.Regions
import com.amazonaws.services.dynamodbv2.{AmazonDynamoDBAsync, AmazonDynamoDBAsyncClientBuilder, AmazonDynamoDBClientBuilder}
import fs2.StreamApp
import io.circe.generic.auto._
import io.github.howardjohn.core.impl.DynamoConfigDatastore
import org.http4s.server.blaze.BlazeBuilder

object Server extends StreamApp[IO] {

  val dynamo = AmazonDynamoDBAsyncClientBuilder
    .standard()
    .withRegion(Regions.US_WEST_2)
    .build()

  val route = new Route(new DynamoConfigDatastore(dynamo))

  def stream(args: List[String], requestShutdown: IO[Unit]) =
    BlazeBuilder[IO]
      .bindHttp(8080, "0.0.0.0")
      .mountService(route.service, "/")
      .serve
}
