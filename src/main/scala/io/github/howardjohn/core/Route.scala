package io.github.howardjohn.core

import cats.effect._
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import io.github.howardjohn.core.config.{ConfigDatastore, ConfigError}
import io.github.howardjohn.core.config.ConfigDatastore.ConfigEntry
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._
import org.http4s.headers.Location
import org.slf4j.LoggerFactory

class Route[T](db: ConfigDatastore)(implicit encoders: Encoder[Seq[ConfigEntry]], encoder: Encoder[ConfigEntry]) {
  import Route._

  private val log = LoggerFactory.getLogger(classOf[Route[T]])

  val service: HttpService[IO] = HttpService[IO] {
    case req @ POST -> Root =>
      for {
        request <- req.decodeJson[CreateNamespaceRequest]
        newName <- db.createNamespace(request.namespace)
        response <- translateErrors[Any](newName)(_ => Ok(""))
      } yield response
    case req @ POST -> Root / namespace =>
      for {
        request <- req.decodeJson[CreateVersionRequest]
        newVersion <- db.getNamespace(namespace).createVersion(request.version)
        location <- IO.fromEither(Uri.fromString(s"/$namespace/${request.version}"))
        response <- translateErrors[Any](newVersion)(_ => Ok("", Location(location)))
      } yield response
    case req @ POST -> Root / namespace / version =>
      for {
        entry <- req.decodeJson[ConfigRequest]
        location <- IO.fromEither(Uri.fromString(s"/$namespace/$version/${entry.key}"))
        result <- db
          .getNamespace(namespace)
          .getVersion(version)
          .write(entry.key, entry.value)
        response <- translateErrors[Unit](result)(_ => Ok("", Location(location)))
      } yield response
    case GET -> Root / namespace / version =>
      db.getNamespace(namespace)
        .getVersion(version)
        .getAll()
        .flatMap(resp => translateErrors(resp)(r => Ok(r.asJson)))
    case GET -> Root / namespace / version / key =>
      db.getNamespace(namespace)
        .getVersion(version)
        .get(key)
        .flatMap(resp =>
          translateErrors(resp) { entry =>
            entry.fold(NotFound())(e => Ok(e.asJson))
        })
    case DELETE -> Root / namespace / version / key =>
      for {
        result <- db
          .getNamespace(namespace)
          .getVersion(version)
          .delete(key)
        response <- translateErrors[Unit](result)(_ => Ok(""))
      } yield response
  }

  def translateErrors[A](resp: Either[ConfigError, A])(f: A => IO[Response[IO]]): IO[Response[IO]] =
    resp.fold(
      err => {
        log.error(s"Config Error: $err")
        InternalServerError()
      },
      f
    )
}

object Route {
  case class CreateNamespaceRequest(
    namespace: String
  )

  case class CreateVersionRequest(
    version: String
  )

  case class ConfigRequest(
    key: String,
    value: Json
  )
}
