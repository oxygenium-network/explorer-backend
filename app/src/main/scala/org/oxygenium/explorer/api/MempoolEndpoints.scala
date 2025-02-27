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
import org.oxygenium.explorer.api.model.{MempoolTransaction, Pagination}

trait MempoolEndpoints extends BaseEndpoint with QueryParams {

  private val mempoolEndpoint =
    baseEndpoint
      .tag("Mempool")
      .in("mempool")

  val listMempoolTransactions: BaseEndpoint[Pagination, ArraySeq[MempoolTransaction]] =
    mempoolEndpoint.get
      .in("transactions")
      .in(pagination)
      .out(jsonBody[ArraySeq[MempoolTransaction]])
      .description("list mempool transactions")
}
