package io.github.howardjohn.core

import cats.effect._
import cats.implicits._
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import io.github.howardjohn.core.ConfigError._
import io.github.howardjohn.core.config.ConfigDatastore
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io.{->, _}
import org.http4s.headers.Location
import org.slf4j.LoggerFactory

class Route[T](db: ConfigDatastore)(implicit encoders: Encoder[Seq[ConfigEntry]], encoder: Encoder[ConfigEntry]) {

  import Route._

  private val log = LoggerFactory.getLogger(classOf[Route[T]])

  private val namespaceService = HttpService[IO] {
    case req @ POST -> Root / "namespace" =>
      for {
        request <- req.decodeJson[CreateNamespaceRequest]
        newName <- db.createNamespace(request.namespace)
        location <- IO.fromEither(Uri.fromString(s"/namespace/${request.namespace}"))
        response <- translateErrors[Any](newName)(_ => Ok("", Location(location)))
      } yield response
    case GET -> Root / "namespace" / namespace =>
      for {
        versions <- db.getNamespace(namespace).getVersions()
        response <- translateErrors(versions)(v => Ok(v.asJson))
      } yield response
  }

  private val tagService = HttpService[IO] {
    case req @ POST -> Root / "namespace" / namespace / "tag" =>
      for {
        request <- req.decodeJson[CreateTagRequest]
        newTag <- db.getNamespace(namespace).createTag(request.tag, request.version)
        location <- IO.fromEither(Uri.fromString(s"/namespace/$namespace/tag/${request.tag}"))
        response <- translateErrors[Any](newTag)(_ => Ok("", Location(location)))
      } yield response
    case GET -> Root / "namespace" / namespace / "tag" =>
      for {
        tags <- db.getNamespace(namespace).getTags()
        response <- translateErrors(tags)(ts => Ok(ts.asJson))
      } yield response
    case GET -> Root / "namespace" / namespace / "tag" / tag =>
      for {
        tagEntry <- db.getNamespace(namespace).getTag(tag).getDetails()
        response <- translateErrors(tagEntry)(t => Ok(t.asJson))
      } yield response
  }

  private val versionService = HttpService[IO] {
    case req @ POST -> Root / "namespace" / namespace / "version" =>
      for {
        request <- req.decodeJson[CreateVersionRequest]
        newVersion <- db.getNamespace(namespace).createVersion(request.version)
        location <- IO.fromEither(Uri.fromString(s"/namespace/$namespace/version/${request.version}"))
        response <- translateErrors[Any](newVersion)(_ => Ok("", Location(location)))
      } yield response
    case GET -> Root / "namespace" / namespace / "version" / version =>
      for {
        versionInfo <- db.getNamespace(namespace).getVersion(version).details()
        response <- translateErrors(versionInfo)(info => Ok(info.asJson))
      } yield response
    case req @ PUT -> Root / "namespace" / namespace / "version" / version =>
      for {
        entry <- req.decodeJson[FreezeVersionRequest]
        result <- if (entry.frozen) {
          db.getNamespace(namespace)
            .getVersion(version)
            .freeze()
        } else {
          IO(Left(FrozenVersion))
        }
        response <- translateErrors[Unit](result)(_ => Ok(""))
      } yield response
  }

  private val configService = HttpService[IO] {
    case req @ POST -> Root / "namespace" / namespace / "version" / version / "config" =>
      for {
        entry <- req.decodeJson[ConfigRequest]
        location <- IO.fromEither(Uri.fromString(s"/namespace/$namespace/version/$version/config/${entry.key}"))
        result <- db
          .getNamespace(namespace)
          .getVersion(version)
          .write(entry.key, entry.value)
        response <- translateErrors[Unit](result)(_ => Ok("", Location(location)))
      } yield response
    case GET -> Root / "namespace" / namespace / "version" / version / "config" =>
      db.getNamespace(namespace)
        .getVersion(version)
        .getAll()
        .flatMap(resp => translateErrors(resp)(r => Ok(r.asJson)))
    case GET -> Root / "namespace" / namespace / "version" / version / "config" / key =>
      db.getNamespace(namespace)
        .getVersion(version)
        .get(key)
        .flatMap(resp =>
          translateErrors(resp) { entry =>
            entry.fold(NotFound())(e => Ok(e.asJson))
        })
    case DELETE -> Root / "namespace" / namespace / "version" / version / "config" / key =>
      for {
        result <- db
          .getNamespace(namespace)
          .getVersion(version)
          .delete(key)
        response <- translateErrors[Unit](result)(_ => Ok(""))
      } yield response
  }

  private val pingService = HttpService[IO] {
    case GET -> Root / "ping" => Ok("pong")
  }

  val service: HttpService[IO] = pingService <+> namespaceService <+> versionService <+> tagService <+> configService

  private def translateErrors[A](resp: Either[ConfigError, A])(f: A => IO[Response[IO]]): IO[Response[IO]] =
    resp.fold(processError, f)

  private def processError(err: ConfigError): IO[Response[IO]] =
    IO(log.error(s"Error encountered: $err"))
      .flatMap(_ =>
        err match {
          case FrozenVersion => MethodNotAllowed()
          case _ => InternalServerError()
      })
}

object Route {
  case class CreateNamespaceRequest(
    namespace: String
  )

  case class CreateTagRequest(
    tag: String,
    version: String
  )

  case class CreateVersionRequest(
    version: String
  )

  case class FreezeVersionRequest(
    frozen: Boolean
  )

  case class ConfigRequest(
    key: String,
    value: Json
  )
}
