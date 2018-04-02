package io.github.howardjohn.config.client

import hammock.circe.implicits._
import io.circe.generic.auto._
import cats._
import cats.effect.IO
import hammock.jvm.Interpreter

object App extends App {
  val config = new HttpConfigDatastore(new HttpClient(Interpreter[IO], "http://127.0.0.1:8080/"))
  println {
    config.getNamespace[String]("example").getVersion("fullVersion").details().value.unsafeRunSync()
  }
}
