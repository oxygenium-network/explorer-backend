// Copyright 2018 The Oxygenium Authors
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

package org.oxygenium.explorer.api

import scala.collection.immutable.ArraySeq

import sttp.tapir._
import sttp.tapir.generic.auto._

import org.oxygenium.api.Endpoints.jsonBody
import org.oxygenium.explorer.api.EndpointExamples._
import org.oxygenium.explorer.api.model._
import org.oxygenium.protocol.model.BlockHash

trait BlockEndpoints extends BaseEndpoint with QueryParams {

  private val blocksEndpoint =
    baseEndpoint
      .tag("Blocks")
      .in("blocks")

  val getBlockByHash: BaseEndpoint[BlockHash, BlockEntry] =
    blocksEndpoint.get
      .in(path[BlockHash]("block_hash"))
      .out(jsonBody[BlockEntry])
      .description("Get a block with hash")

  val getBlockTransactions: BaseEndpoint[(BlockHash, Pagination), ArraySeq[Transaction]] =
    blocksEndpoint.get
      .in(path[BlockHash]("block_hash"))
      .in("transactions")
      .in(pagination)
      .out(jsonBody[ArraySeq[Transaction]])
      .description("Get block's transactions")

  val listBlocks: BaseEndpoint[Pagination.Reversible, ListBlocks] =
    blocksEndpoint.get
      .in(paginationReversible)
      .out(jsonBody[ListBlocks])
      .description("List latest blocks")
}
