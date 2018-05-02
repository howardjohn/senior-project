package io.github.howardjohn.config.client

import hammock.circe.implicits._
import io.circe.generic.auto._
import cats._
import cats.effect.IO
import hammock.jvm.Interpreter
import io.circe.Json

object App extends App {
  val config = new HttpConfigDatastore(new HttpClient(Interpreter[IO], "http://127.0.0.1:8888/"))
  println {
    config.getNamespace[String]("example").getVersion("fullVersion").details().value.unsafeRunSync()
  }
}
