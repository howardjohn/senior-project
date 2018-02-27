package io.github.howardjohn.core

import cats.data.{EitherT, Kleisli, OptionT}
import cats.effect._
import cats.implicits._
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import io.github.howardjohn.core.ConfigError._
import io.github.howardjohn.core.config.ConfigDatastore
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._
import org.http4s.headers.Location
import org.slf4j.LoggerFactory

class Route[T](db: ConfigDatastore)(implicit encoders: Encoder[Seq[ConfigEntry]], encoder: Encoder[ConfigEntry]) {

  import Route._

  private val log = LoggerFactory.getLogger(classOf[Route[T]])

  private val namespaceService = HttpService[IO] {
    case req @ POST -> Root / "namespace" =>
      translateLocation(for {
        request <- parseJson[CreateNamespaceRequest](req)
        newName <- db.createNamespace(request.namespace)
        location <- makeUri(s"/namespace/${request.namespace}")
      } yield location)
    case GET -> Root / "namespace" / namespace =>
      translateJson(db.getNamespace(namespace).getVersions())
  }

  private val tagService = HttpService[IO] {
    case req @ POST -> Root / "namespace" / namespace / "tag" =>
      translateLocation(for {
        request <- parseJson[CreateTagRequest](req)
        newTag <- db.getNamespace(namespace).createTag(request.tag, request.version)
        location <- makeUri(s"/namespace/$namespace/tag/${request.tag}")
      } yield location)
    case GET -> Root / "namespace" / namespace / "tag" =>
      translateJson(db.getNamespace(namespace).getTags())
    case req @ PUT -> Root / "namespace" / namespace / "tag" / tag =>
      translateUnit(for {
        version <- parseJson[MoveTagRequest](req)
        result <- db
          .getNamespace(namespace)
          .getTag(tag)
          .moveTag(version.version)
      } yield result)
    case GET -> Root / "namespace" / namespace / "tag" / tag =>
      translateOptionalJson(db.getNamespace(namespace).getTag(tag).getDetails())
  }

  private val versionService = HttpService[IO] {
    case req @ POST -> Root / "namespace" / namespace / "version" =>
      translateLocation(for {
        request <- parseJson[CreateVersionRequest](req)
        newVersion <- db.getNamespace(namespace).createVersion(request.version)
        location <- makeUri(s"/namespace/$namespace/version/${request.version}")
      } yield location)
    case GET -> Root / "namespace" / namespace / "version" / version =>
      translateOptionalJson(db.getNamespace(namespace).getVersion(version).details())
    case req @ PUT -> Root / "namespace" / namespace / "version" / version =>
      translateUnit(for {
        entry <- parseJson[FreezeVersionRequest](req)
        result <- if (entry.frozen) {
          db.getNamespace(namespace)
            .getVersion(version)
            .freeze()
        } else {
          EitherT.leftT[IO, Unit](FrozenVersion: ConfigError)
        }
      } yield result)
  }

  private val configService = HttpService[IO] {
    case req @ POST -> Root / "namespace" / namespace / "version" / version / "config" =>
      translateLocation(for {
        entry <- parseJson[ConfigRequest](req)
        location <- makeUri(s"/namespace/$namespace/version/$version/config/${entry.key}")
        result <- db
          .getNamespace(namespace)
          .getVersion(version)
          .write(entry.key, entry.value)
      } yield location)
    case GET -> Root / "namespace" / namespace / "version" / version / "config" =>
      translateJson(db.getNamespace(namespace).getVersion(version).getAll())
    case GET -> Root / "namespace" / namespace / "version" / version / "config" / key =>
      translateOptionalJson(db.getNamespace(namespace).getVersion(version).get(key))
    case DELETE -> Root / "namespace" / namespace / "version" / version / "config" / key =>
      translateUnit(db.getNamespace(namespace).getVersion(version).delete(key))
  }

  private val pingService = HttpService[IO] {
    case GET -> Root / "ping" => Ok("pong")
  }

  def tagAsVersionMiddleware(service: HttpService[IO]): HttpService[IO] = Kleisli { req: Request[IO] =>
    req match {
      case _ -> "namespace" /: namespace /: "tag" /: tag /: rest =>
        OptionT.liftF {
          translate(
            for {
              tagEntry <- orNotFound(db.getNamespace(namespace).getTag(tag).getDetails())
              uri <- makeUri(s"namespace/$namespace/version/${tagEntry.version}$rest")
              newReq = req.withUri(uri)
            } yield service.orNotFound(req.withUri(uri))
          )(identity)
        }
      case _ => service(req)
    }
  }

  val service: HttpService[IO] = pingService <+> namespaceService <+> tagService <+> versionService <+>
    tagAsVersionMiddleware(configService)

  private def translate[A](resp: Result[A])(f: A => IO[Response[IO]]): IO[Response[IO]] =
    resp.fold(processError, f).flatten

  private def translateJson[A: Encoder](resp: Result[A]): IO[Response[IO]] =
    translate(resp)(r => Ok(r.asJson))

  private def translateOptionalJson[A: Encoder](resp: Result[Option[A]]): IO[Response[IO]] =
    translate(resp)(a => a.map(entry => Ok(entry.asJson)).getOrElse(NotFound()))

  private def translateLocation(resp: Result[Uri]): IO[Response[IO]] =
    translate(resp)(loc => Ok("", Location(loc)))

  private def translateUnit[A](resp: Result[A]): IO[Response[IO]] =
    translate(resp)(_ => Ok(""))

  private def parseJson[A: Decoder](req: Request[IO]): Result[A] =
    EitherT.liftF[IO, ConfigError, A](req.decodeJson[A])

  private def processError(err: ConfigError): IO[Response[IO]] =
    err match {
      case FrozenVersion => MethodNotAllowed()
      case MissingEntry => NotFound()
      case _ => IO(log.error(s"Error encountered: $err")).flatMap(_ => InternalServerError())
    }

  private def orNotFound[A](item: Result[Option[A]]): Result[A] = EitherT {
    item.value.map {
      case Right(Some(value)) => Right(value)
      case Right(None) => Left(MissingEntry)
      case Left(error) => Left(error)
    }
  }

  private def makeUri(uri: String): Result[Uri] =
    EitherT.fromEither[IO] {
      Uri
        .fromString(uri)
        .fold(
          error => Left(UnknownError(s"Couldn't make uri from $uri")),
          right => Right(right)
        )
    }
}

object Route {
  case class CreateNamespaceRequest(
    namespace: String
  )

  case class CreateTagRequest(
    tag: String,
    version: String
  )

  case class MoveTagRequest(
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
