package io.github.howardjohn.config.client

import cats._
import cats.effect.IO
import hammock.jvm.Interpreter

object App extends App {
  val config = new HttpConfigDatastore(new HttpClient(Interpreter[IO], "http://127.0.0.1:8080/"))
  implicit val stringId: Id[String] = ""
  println {
    config.createNamespace[String]("blah").value.unsafeRunSync()
  }
}
