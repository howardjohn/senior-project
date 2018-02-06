package io.github.howardjohn.core

import cats.effect._
import io.circe._
import io.circe.syntax._
import io.github.howardjohn.core.ConfigDatastore.ConfigEntry
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._
import org.http4s.headers.Location

import io.circe.generic.auto._
class Route[T](dbCreator: String => ConfigDatastore)(
  implicit encoders: Encoder[Seq[ConfigEntry]],
  encoder: Encoder[ConfigEntry]) {
  implicit val entryDecoder = jsonOf[IO, ConfigEntry]
  implicit val jsonDecoder = jsonOf[IO, Json]

  val service: HttpService[IO] = HttpService[IO] {
    case GET -> Root / namespace => dbCreator(namespace).getAll().flatMap(entries => Ok(entries.asJson))
    case GET -> Root / namespace / key =>
      dbCreator(namespace).get(key).flatMap {
        case Some(v) => Ok(v.asJson)
        case None => NotFound()
      }
    case DELETE -> Root / namespace / key =>
      for {
        _ <- dbCreator(namespace).delete(key)
        resp <- Ok("")
      } yield resp
    case req @ PUT -> Root / namespace / key =>
      for {
        entry <- req.as[Json]
        _ <- dbCreator(namespace).update(key, entry)
        response <- Ok("")
      } yield response
    case req @ POST -> Root / namespace =>
      for {
        entry <- req.as[ConfigEntry]
        _ <- dbCreator(namespace).write(entry.key, entry.value)
        location <- IO.fromEither(Uri.fromString(s"/${entry.key}"))
        response <- Ok("", Location(location))
      } yield response
    case GET -> Root / "ping" => Ok("pong")
  }
}
