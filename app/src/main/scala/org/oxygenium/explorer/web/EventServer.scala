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

package org.oxygenium.explorer.web

import scala.collection.immutable.ArraySeq
import scala.concurrent.{ExecutionContext, Future}

import io.vertx.ext.web._
import slick.basic.DatabaseConfig
import slick.jdbc.PostgresProfile

import org.oxygenium.explorer.api.EventsEndpoints
import org.oxygenium.explorer.persistence.DBRunner._
import org.oxygenium.explorer.persistence.queries.EventQueries._

class EventServer(implicit
    val executionContext: ExecutionContext,
    dc: DatabaseConfig[PostgresProfile]
) extends Server
    with EventsEndpoints {
  val routes: ArraySeq[Router => Route] =
    ArraySeq(
      route(getEventsByTxId.serverLogicSuccess[Future] { txId =>
        run(getEventsByTxIdQuery(txId)).map(_.map(_.toApi))
      }),
      route(getEventsByContractAddress.serverLogicSuccess[Future] {
        case (address, eventIndex, pagination) =>
          run(getEventsByContractAddressQuery(address, eventIndex, pagination)).map(_.map(_.toApi))
      }),
      route(getEventsByContractAndInputAddress.serverLogicSuccess[Future] {
        case (contract, input, eventIndex, pagination) =>
          run(getEventsByContractAndInputAddressQuery(contract, input, eventIndex, pagination))
            .map(_.map(_.toApi))
      })
    )
}
