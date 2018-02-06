package io.github.howardjohn.core

import cats.effect.IO
import com.amazonaws.regions.Regions
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import fs2.StreamApp
import io.circe.generic.auto._
import io.github.howardjohn.core.ConfigDatastore.ConfigEntry
import org.http4s.circe._
import org.http4s.server.blaze.BlazeBuilder

import com.gu.scanamo._
object Server extends StreamApp[IO] {

  val dynamo = AmazonDynamoDBClientBuilder
    .standard()
    .withRegion(Regions.US_WEST_2)
    .build()

  implicit val jsonFormt = DynamoConfigDatastore.jsonFormat
  val route = new Route(table => new DynamoConfigDatastore(dynamo, Table[ConfigEntry](table)))

  def stream(args: List[String], requestShutdown: IO[Unit]) =
    BlazeBuilder[IO]
      .bindHttp(8080, "0.0.0.0")
      .mountService(route.service, "/")
      .serve
}
