package com.ariskk.toychain4s.client

import sttp.model._
import sttp.client3._
import sttp.client3.httpclient.zio._
import sttp.capabilities.zio.ZioStreams
import sttp.capabilities.WebSockets
import zio.json._
import zio._

import com.ariskk.toychain4s.model._
import com.ariskk.toychain4s.utils.JsonCodecs
import Codecs._

object Client {

  case class HostConfig(host: String, port: Int) {
    lazy val hostString = s"$host:$port"
  }
  case class Deps(config: HostConfig, httpClient: SttpBackend[Task, ZioStreams with WebSockets])

  object Deps {
    def fromHostConfig(hc: HostConfig) = HttpClientZioBackend().map { be =>
      Deps(hc, be)
    }
  }

  case class ClientError(m: String, cause: Throwable) extends Exception(m, cause)

  type DispatchReq[R] = ZIO[Deps, ClientError, R]

  case class ClientRequest[T: JsonEncoder](
    path: Path,
    method: Method,
    body: Option[T] = None
  ) {
    lazy val requestString = s"$method ${path.path}"
  }

  case class ServerResponse[R: JsonDecoder](
    status: StatusCode,
    response: R
  )

  private def processServerResponse[R: JsonDecoder](
    response: Response[Either[String, String]]
  ) =
    ZIO
      .fromEither(
        response.body.left
          .map(e => new Exception(s"Response error $e"))
          .flatMap(JsonCodecs.decode[R](_))
      )
      .map(r => ServerResponse[R](response.code, r))
      .mapError(e => ClientError("Failed to decode response", e))

  def get[R: JsonDecoder](path: Path) =
    ZIO.accessM { deps: Deps =>
      basicRequest
        .get(
          uri"http://${deps.config.hostString}/${path.path}"
        )
        .send(deps.httpClient)
        .flatMap(processServerResponse[R](_))
    }

  def post[T: JsonEncoder, R: JsonDecoder](path: Path, body: T) =
    ZIO.accessM { deps: Deps =>
      basicRequest
        .body(JsonCodecs.encode(body))
        .post(
          uri"http://${deps.config.hostString}/${path.path}"
        )
        .send(deps.httpClient)
        .flatMap(processServerResponse[R](_))
    }

  // Block APIs

  def getBlocks = get[List[Block]](Path.Blocks)

  def createBlock(command: BlockCommand) =
    post[BlockCommand, Block](Path.Blocks, command)

}
