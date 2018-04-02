package io.github.howardjohn.config.client

import cats.data.EitherT
import cats.effect.IO
import cats.free.Free
import hammock._
import hammock.circe.implicits._
import io.circe.generic.auto._
import io.github.howardjohn.config.ConfigError._
import io.github.howardjohn.config.{ConfigError, Result}

class HttpClient(interpeter: InterpTrans[IO], baseUri: String) {
  import HttpClient._
  implicit val interp = interpeter

  def get[T: Decoder](uri: String): Result[Option[T]] =
    runRequest[T](Hammock.request(Method.GET, getUri(uri), Map())) { http =>
      parseJson[T](http.entity)
    }

  def delete[I: Codec](uri: String): Result[Option[Unit]] =
    runRequest[Unit](Hammock.request(Method.DELETE, getUri(uri), Map())) { http =>
      Right(Unit)
    }

  def put[I: Codec](uri: String, body: I): Result[Option[Unit]] =
    runRequest[Unit](Hammock.request(Method.PUT, getUri(uri), Map(), Some(body))) { http =>
      Right(Unit)
    }

  def post[I: Codec](uri: String, body: I): Result[Option[String]] =
    runRequest[Option[String]](Hammock.request(Method.POST, getUri(uri), Map(), Some(body))) { http =>
      Right(http.headers.get("Location"))
    }.map(_.flatten)

  private def getUri(uri: String): Uri =
    Uri.unsafeParse(s"$baseUri$uri")

  private def runRequest[T: Decoder](request: Free[HttpF, HttpResponse])(
    success: HttpResponse => Either[ConfigError, T]): Result[Option[T]] =
    EitherT {
      request
        .exec[IO]
        .map { http =>
          http.status.code match {
            case 200 => success(http).map(t => Some(t))
            case 404 => Right(None)
            case 500 => Left(UnknownError("There was an unknown error."))
            case _ => Left(parseError(http.entity))
          }
        }
    }
}

object HttpClient {
  def parseError(data: Entity): ConfigError =
    parseJson[ErrorMessage](data)
      .fold(identity, processError)

  private def processError(err: ErrorMessage) = err.error match {
    case "IllegalWrite" => IllegalWrite(err.details)
    case "MissingField" => MissingField(err.details)
    case "UnknownError" => UnknownError(err.details)
    case "ReadError" => ReadError(err.details)
  }

  def parseJson[T](data: Entity)(implicit dec: Decoder[T]): Either[ConfigError, T] =
    dec.decode(data).left.map(e => ReadError(s"Failed to parse response: ${e.message}"))

  case class ErrorMessage(
    error: String,
    details: String
  )
}
