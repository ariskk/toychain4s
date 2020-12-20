package com.ariskk.toychain4s.api

import zio._
import zio.stream._
import zio.json._
import uzhttp.{ HTTPError, Request, Response, Status }

import com.ariskk.toychain4s.service.Service.Result
import com.ariskk.toychain4s.utils.JsonCodecs
import com.ariskk.toychain4s.service._

trait Route {

  def requestHandler: PartialFunction[Request, Result[Response]]

  def bodyResponse[T: JsonEncoder](t: T): Response = Response.const(
    body = JsonCodecs.encode(t).getBytes(),
    contentType = "application/json",
    status = Status.Ok
  )

  private def decodeBody[T: JsonDecoder](byteStream: Stream[HTTPError, Byte]): IO[Throwable, T] =
    for {
      bodyString <- byteStream.runCollect.map { chunks =>
        val byteArray = Chunk.fromIterable(chunks).toArray
        new String(byteArray)
      }
      decoded <- ZIO.fromEither(JsonCodecs.decode[T](bodyString))
    } yield decoded

  def withBody[T: JsonDecoder, R: JsonEncoder](request: Request)(f: T => Result[R]): Result[Response] =
    request.body.fold[Result[Response]](
      ZIO.fail(InvalidBlockDataError("Body expected but not provided", None))
    ) { bodyBytes =>
      for {
        decoded <- decodeBody(bodyBytes).mapError { e =>
          InvalidBlockDataError("Malformed body", Option(e))
        }
        result <- f(decoded)
      } yield bodyResponse(result)
    }

  def handler: PartialFunction[Request, ZIO[Dependencies, HTTPError, Response]] = { request =>
    requestHandler(request).mapError {
      case NotFoundError               => HTTPError.NotFound(request.uri.toString)
      case InternalServerError(_, _)   => HTTPError.InternalServerError("Oh great", None)
      case InvalidBlockDataError(m, _) => HTTPError.BadRequest(m)
    }
  }

}
