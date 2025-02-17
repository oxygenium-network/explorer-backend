// Copyright 2018 The Alephium Authors
// This file is part of the oxygenium project.
//
// The library is free software: you can redistribute it and/or modify
// it under the terms of the GNU Lesser General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// The library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public License
// along with the library. If not, see <http://www.gnu.org/licenses/>.

package org.oxygenium.explorer.web

import scala.collection.immutable.ArraySeq
import scala.concurrent.{ExecutionContext, Future}

import io.vertx.ext.web._
import slick.basic.DatabaseConfig
import slick.jdbc.PostgresProfile

import org.oxygenium.api.ApiError
import org.oxygenium.explorer.api.BlockEndpoints
import org.oxygenium.explorer.cache.BlockCache
import org.oxygenium.explorer.service.BlockService

class BlockServer(implicit
    val executionContext: ExecutionContext,
    dc: DatabaseConfig[PostgresProfile],
    blockCache: BlockCache
) extends Server
    with BlockEndpoints {
  val routes: ArraySeq[Router => Route] =
    ArraySeq(
      route(listBlocks.serverLogicSuccess[Future](BlockService.listBlocks(_))),
      route(getBlockByHash.serverLogic[Future] { hash =>
        BlockService
          .getBlockByHash(hash)
          .map(_.toRight(ApiError.NotFound(hash.value.toHexString)))
      }),
      route(getBlockTransactions.serverLogicSuccess[Future] { case (hash, pagination) =>
        BlockService.getBlockTransactions(hash, pagination)
      })
    )
}
