package io.github.howardjohn.config.backend

import cats.data.{EitherT, Kleisli, OptionT}
import cats.effect._
import cats.implicits._
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import io.github.howardjohn.config.ConfigError._
import io.github.howardjohn.config._
import io.github.howardjohn.config.backend.impl.DynamoConfigDatastore
import io.github.howardjohn.config.backend.impl.Scanamo.jsonFormat
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._
import org.http4s.headers.Location
import org.slf4j.LoggerFactory

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
    case GET -> Root / "namespace" / namespace =>
      translateJson(db.getNamespace[Json](namespace).getVersions())
  }

  object Discriminator extends QueryParamDecoderMatcher[String]("discriminator")
  private val tagService = HttpService[IO] {
    case req @ POST -> Root / "tag" =>
      translateLocation(for {
        request <- parseJson[CreateTagRequest](req)
        newTag <- db.createTag(request.tag, request.namespace)
        location <- makeUri(s"/tag/${request.tag}/namespace/${request.namespace}")
      } yield location)
    case req @ PUT -> Root / "tag" / tag / "namespace" / namespace =>
      translateUnit(for {
        req <- parseJson[Map[String, Int]](req)
        result <- db
          .getTag(tag)
          .moveTag(namespace, req)
      } yield result)
    case GET -> Root / "tag" / tag / "namespace" / namespace :? Discriminator(discriminator) =>
      translateOptionalJson(db.getTag(tag).getDetails(namespace, discriminator))
    case GET -> Root / "tag" / tag / "namespace" / namespace =>
      translateOptionalJson(db.getTag(tag).getDetails(namespace))
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
              tagEntry <- orNotFound(db.getTag(tag).getDetails(namespace, discriminator))
              uri <- makeUri(s"namespace/$namespace/version/${tagEntry.version}/config$rest")
            } yield service.orNotFound(req.withUri(uri))
          )(identity)
        }
      case _ -> "namespace" /: namespace /: "tag" /: tag /: "config" /: rest =>
        OptionT.liftF {
          translate(
            for {
              tagEntry <- orNotFound(db.getTag(tag).getDetails(namespace))
              uri <- makeUri(s"namespace/$namespace/version/${tagEntry.version}/config$rest")
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
    namespace: String
  )

  case class MoveTagRequest(
    version: String,
    weight: Int
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