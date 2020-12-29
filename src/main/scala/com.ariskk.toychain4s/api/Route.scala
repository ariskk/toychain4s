package com.ariskk.toychain4s.api

import scala.util.Try

import zio._
import zio.stream._
import zio.clock.Clock
import zio.json._
import uzhttp.{ HTTPError, Request, Response, Status }

import com.ariskk.toychain4s.service.Service.Result
import com.ariskk.toychain4s.utils.JsonCodecs
import com.ariskk.toychain4s.service._
import com.ariskk.toychain4s.model.{ Cursor, Index, Page }

trait Route {

  def requestHandler: PartialFunction[Request, Result[Response]]

  def bodyResponse[T: JsonEncoder](t: T): Response = Response.const(
    body = JsonCodecs.encode(t).getBytes("UTF-8"),
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

  def withPage[R: JsonEncoder](request: Request)(f: Page => Result[List[R]]): Result[Response] = {
    val queryParts = request.uri.getQuery.split("&")
    val from = queryParts.find(_.contains("from")).map { case s"from=$cursor" =>
      Cursor(cursor)
    }
    val size = queryParts.find(_.contains("size")).flatMap { case s"size=$size" =>
      Try(Integer.parseInt(size)).toOption
    }
    val page = Page(from, size.getOrElse(10))

    f(page).map(bodyResponse(_))
  }

  def handler: PartialFunction[Request, ZIO[Clock with Has[Module], HTTPError, Response]] = { request =>
    requestHandler(request).mapError {
      case NotFoundError                 => HTTPError.NotFound(request.uri.toString)
      case InternalServerError(m, cause) => HTTPError.InternalServerError(s"Oh great: $m", cause)
      case InvalidBlockDataError(m, _)   => HTTPError.BadRequest(m)
    }
  }

}
