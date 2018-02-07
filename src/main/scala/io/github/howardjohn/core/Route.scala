package io.github.howardjohn.core

import cats.effect._
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import io.github.howardjohn.core.ConfigDatastore.ConfigEntry
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._
import org.http4s.headers.Location

class Route[T](db: ConfigDatastore)(implicit encoders: Encoder[Seq[ConfigEntry]], encoder: Encoder[ConfigEntry]) {

  import Route._

  val service: HttpService[IO] = HttpService[IO] {
    case GET -> Root / "config" / namespace => db.getAll(namespace).flatMap(entries => Ok(entries.asJson))
    case GET -> Root / "config" / namespace / key =>
      db.get(namespace)(key).flatMap {
        case Some(v) => Ok(v.asJson)
        case None => NotFound()
      }
    case DELETE -> Root / "config" / namespace / key =>
      for {
        _ <- db.delete(namespace)(key)
        resp <- Ok("")
      } yield resp
    case req @ PUT -> Root / "config" / namespace / key =>
      for {
        entry <- req.decodeJson[Json]
        _ <- db.update(namespace)(key, entry)
        response <- Ok("")
      } yield response
    case req @ POST -> Root / "config" / namespace =>
      for {
        entry <- req.decodeJson[ConfigEntry]
        _ <- db.write(namespace)(entry.key, entry.value)
        location <- IO.fromEither(Uri.fromString(s"/config/$namespace/${entry.key}"))
        response <- Ok("", Location(location))
      } yield response
    case req @ POST -> Root / "config" =>
      for {
        entry <- req.decodeJson[CreateNamespaceRequest]
        _ <- db.createNamespace(entry.namespace)
        location <- IO.fromEither(Uri.fromString(s"/${entry.namespace}"))
        response <- Ok("", Location(location))
      } yield response
    case GET -> Root / "ping" => Ok("pong")
  }
}

object Route {
  case class CreateNamespaceRequest(
    namespace: String
  )
}
