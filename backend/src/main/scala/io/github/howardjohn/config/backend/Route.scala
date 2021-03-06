package io.github.howardjohn.config.backend

import cats.data.{EitherT, Kleisli, OptionT}
import cats.effect._
import cats.implicits._
import io.circe._
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import io.github.howardjohn.config.ConfigError.{IllegalWrite, MissingField, ReadError, UnknownError}
import io.github.howardjohn.config.Request._
import io.github.howardjohn.config._
import io.github.howardjohn.config.backend.impl.DynamoConfigDatastore
import io.github.howardjohn.config.backend.impl.Scanamo.jsonFormat
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._
import org.http4s.headers.Location
import org.http4s.server.middleware.{CORS, CORSConfig, Logger}
import org.slf4j.LoggerFactory
import scala.concurrent.duration._

class Route[T](db: DynamoConfigDatastore)(
  implicit encoders: Encoder[Seq[ConfigEntry[Json]]],
  encoder: Encoder[ConfigEntry[Json]]) {

  import Route._

  private val log = LoggerFactory.getLogger(classOf[Route[T]])

  private val namespaceService = HttpService[IO] {
    case req @ POST -> Root / "namespace" =>
      translateLocation(for {
        request <- parseJson[CreateNamespaceRequest](req)
        _ <- db.createNamespace[Json](request.namespace)
        location <- makeUri(s"/namespace/${request.namespace}")
      } yield location)
    case GET -> Root / "namespace" =>
      translateJson(db.getAllNamespaces())
    case GET -> Root / "namespace" / namespace =>
      translateJson(db.getNamespace[Json](namespace).getVersions())
  }

  object Discriminator extends QueryParamDecoderMatcher[String]("discriminator")
  private val tagService = HttpService[IO] {
    case req @ POST -> Root / "tag" =>
      translateLocation(for {
        request <- parseJson[CreateTagRequest](req)
        newTag <- db.getNamespace(request.namespace).createTag(request.tag)
        location <- makeUri(s"/tag/${request.tag}/namespace/${request.namespace}")
      } yield location)
    case req @ PUT -> Root / "tag" / tag / "namespace" / namespace =>
      translateUnit(for {
        req <- parseJson[Map[String, Int]](req)
        result <- db
          .getNamespace(namespace)
          .getTag(tag)
          .moveTag(req)
      } yield result)
    case GET -> Root / "tag" / tag / "namespace" / namespace :? Discriminator(discriminator) =>
      translateOptionalJson(db.getNamespace(namespace).getTag(tag).getDetails(discriminator))
    case GET -> Root / "tag" / tag / "namespace" / namespace =>
      translateOptionalJson(db.getNamespace(namespace).getTag(tag).getDetails())
  }

  private val versionService = HttpService[IO] {
    case req @ POST -> Root / "namespace" / namespace / "version" =>
      translateLocation(for {
        request <- parseJson[CreateVersionRequest](req)
        _ <- db.getNamespace[Json](namespace).createVersion(request.version)
        location <- makeUri(s"/namespace/$namespace/version/${request.version}")
      } yield location)
    case GET -> Root / "namespace" / namespace / "version" / version =>
      translateOptionalJson(db.getNamespace(namespace).getVersion(version).details())
    case req @ PUT -> Root / "namespace" / namespace / "version" / version =>
      translateUnit(for {
        entry <- parseJson[FreezeVersionRequest](req)
        result <- if (entry.frozen)
          db.getNamespace(namespace)
            .getVersion(version)
            .freeze()
        else
          EitherT.leftT[IO, Unit](IllegalWrite("Cannot unfreeze a version."): ConfigError)
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
      translateJson(db.getNamespace[Json](namespace).getVersion(version).getAll())
    case GET -> Root / "namespace" / namespace / "version" / version / "config" / key =>
      translateOptionalJson(db.getNamespace[Json](namespace).getVersion(version).get(key))
    case DELETE -> Root / "namespace" / namespace / "version" / version / "config" / key =>
      translateUnit(db.getNamespace[Json](namespace).getVersion(version).delete(key))
  }

  private val pingService = HttpService[IO] {
    case GET -> Root / "ping" => Ok("pong")
  }

  def tagAsVersionMiddleware(service: HttpService[IO]): HttpService[IO] = Kleisli { req: Request[IO] =>
    req match {
      case _ -> "namespace" /: namespace /: "tag" /: tag /: "config" /: rest :? Discriminator(discriminator) =>
        OptionT.liftF {
          translate(
            for {
              tagEntry <- orNotFound(db.getNamespace(namespace).getTag(tag).getDetails(discriminator))
              uri <- makeUri(s"namespace/$namespace/version/${tagEntry.version}/config$rest")
            } yield service.orNotFound(req.withUri(uri))
          )(identity)
        }
      case _ -> "namespace" /: namespace /: "tag" /: tag /: "config" /: rest =>
        OptionT.liftF {
          translate(
            for {
              tagEntry <- orNotFound(db.getNamespace(namespace).getTag(tag).getDetails())
              uri <- makeUri(s"namespace/$namespace/version/${tagEntry.version}/config$rest")
            } yield service.orNotFound(req.withUri(uri))
          )(identity)
        }
      case _ => service(req)
    }
  }

  val service: HttpService[IO] = CORS(
    pingService <+> namespaceService <+> tagService <+> versionService <+>
      tagAsVersionMiddleware(configService),
    CORS.DefaultCORSConfig)

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
    EitherT {
      req.as[String].map { data =>
        decode[A](data).left.map(e => MissingField(e.getMessage))
      }
    }

  private def processError(err: ConfigError): IO[Response[IO]] =
    err match {
      case ConfigError.NotFound => NotFound()
      case IllegalWrite(msg) => MethodNotAllowed(ErrorMessage("IllegalWrite", msg).asJson)
      case MissingField(msg) => BadRequest(ErrorMessage("MissingField", msg).asJson)
      case UnknownError(msg) => InternalServerError(ErrorMessage("UnknownError", msg).asJson)
      case ReadError(msg) => InternalServerError(ErrorMessage("ReadError", msg).asJson)
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
  case class ErrorMessage(
    error: String,
    details: String
  )
}
