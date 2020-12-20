package com.ariskk.toychain4s.api

import uzhttp.Request.Method._
import uzhttp.{ Request, Response }

import com.ariskk.toychain4s.service.BlockService
import com.ariskk.toychain4s.service.Service.Result
import com.ariskk.toychain4s.model.Codecs._
import com.ariskk.toychain4s.model.{ Block, BlockCommand }

object Api extends Route {

  def requestHandler: PartialFunction[Request, Result[Response]] = { request =>
    (request.method, request.uri.getPath) match {
      case (GET, "/blocks")  => BlockService.fetchBlockchain.map(bodyResponse(_))
      case (POST, "/blocks") => withBody[BlockCommand, Block](request)(BlockService.createNextBlock(_))
    }
  }

}
