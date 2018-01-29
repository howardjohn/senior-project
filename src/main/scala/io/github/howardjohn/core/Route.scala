package io.github.howardjohn.core

import cats.effect._
import org.http4s._, org.http4s.dsl.io._, org.http4s.implicits._

class Route {

  val service: HttpService[IO] = HttpService[IO] {
    case GET -> Root => Ok("hello")
  }

}
