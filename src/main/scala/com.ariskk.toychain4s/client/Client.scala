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

  type Deps = SttpBackend[Task, ZioStreams with WebSockets]

  case class ClientError(m: String, cause: Throwable) extends Exception(m, cause)

  type DispatchReq[R] = ZIO[Deps, ClientError, R]

  case class ClientRequest[T: JsonEncoder](
    host: Host,
    path: Path,
    method: Method,
    body: Option[T] = None
  ) {
    lazy val requestString = s"$method ${host.hostString} ${path.path}"
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

  def get[R: JsonDecoder](host: Host, path: Path) =
    ZIO.accessM { deps: Deps =>
      basicRequest
        .get(
          uri"http://${host.hostString}/${path.path}"
        )
        .send(deps)
        .flatMap(processServerResponse[R](_))
    }

  def post[T: JsonEncoder, R: JsonDecoder](host: Host, path: Path, body: T) =
    ZIO.accessM { deps: Deps =>
      basicRequest
        .body(JsonCodecs.encode(body))
        .post(
          uri"http://${host.hostString}/${path.path}"
        )
        .send(deps)
        .flatMap(processServerResponse[R](_))
    }

  object ApiIo {

    def createBlock(host: Host, command: BlockCommand) =
      post[BlockCommand, Block](host, Path.Commands, command)

    def replicateBlock(host: Host, block: Block) =
      post[Block, Block](host, Path.Blocks, block)

    def getBlocks(host: Host) = get[List[Block]](host, Path.Blocks)

    def getPeers(host: Host) = get[List[Peer]](host, Path.Peers)

    def addPeer(host: Host, p: Peer) = post[Peer, Peer](host, Path.Peers, p)

  }

}
