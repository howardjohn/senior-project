package io.github.howardjohn.core

import cats.effect.IO
import fs2.StreamApp
import org.http4s.server.blaze.BlazeBuilder

object Server extends StreamApp[IO]{
  val service = new Route().service

  def stream(args: List[String], requestShutdown: IO[Unit]) =
    BlazeBuilder[IO]
      .bindHttp(8080, "0.0.0.0")
      .mountService(service, "/")
      .serve
}
