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
  val db = dbCreator("example")
  implicit val entryDecoder = jsonOf[IO, ConfigEntry]
  implicit val jsonDecoder = jsonOf[IO, Json]

  val service: HttpService[IO] = HttpService[IO] {
    case GET -> Root => db.getAll().flatMap(entries => Ok(entries.asJson))
    case GET -> Root / key =>
      db.get(key).flatMap {
        case Some(v) => Ok(v.asJson)
        case None => NotFound()
      }
    case DELETE -> Root / key =>
      for {
        _ <- db.delete(key)
        resp <- Ok("")
      } yield resp
    case req @ PUT -> Root / key =>
      for {
        entry <- req.as[Json]
        _ <- db.update(key, entry)
        response <- Ok("")
      } yield response
    case req @ POST -> Root =>
      for {
        entry <- req.as[ConfigEntry]
        _ <- db.write(entry.key, entry.value)
        location <- IO.fromEither(Uri.fromString(s"/${entry.key}"))
        response <- Ok("", Location(location))
      } yield response
    case GET -> Root / "ping" => Ok("pong")
  }
}
