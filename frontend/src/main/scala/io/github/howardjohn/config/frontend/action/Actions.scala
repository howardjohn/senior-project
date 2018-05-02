package io.github.howardjohn.config.frontend.action

import cats.effect.IO
import hammock.js.Interpreter
import io.github.howardjohn.config.client.{HttpClient, HttpConfigDatastore}

object Actions {
  val client = new HttpConfigDatastore(new HttpClient(Interpreter[IO], "http://127.0.0.1:8888/"))
}
