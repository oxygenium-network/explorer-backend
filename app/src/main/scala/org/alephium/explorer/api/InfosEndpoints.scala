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

import java.math.BigDecimal

import scala.collection.immutable.ArraySeq

import sttp.tapir._
import sttp.tapir.generic.auto._

import org.oxygenium.api.Endpoints.jsonBody
import org.oxygenium.explorer.api.EndpointExamples._
import org.oxygenium.explorer.api.model._

// scalastyle:off magic.number
trait InfosEndpoints extends BaseEndpoint with QueryParams {

  private val infosEndpoint =
    baseEndpoint
      .tag("Infos")
      .in("infos")

  private val supplyEndpoint =
    infosEndpoint
      .in("supply")

  val getInfos: BaseEndpoint[Unit, ExplorerInfo] =
    infosEndpoint.get
      .out(jsonBody[ExplorerInfo])
      .description("Get explorer informations")

  val listTokenSupply: BaseEndpoint[Pagination, ArraySeq[TokenSupply]] =
    supplyEndpoint.get
      .in(pagination)
      .out(jsonBody[ArraySeq[TokenSupply]])
      .description("Get token supply list")

  val getCirculatingSupply: BaseEndpoint[Unit, BigDecimal] =
    supplyEndpoint.get
      .in("circulating-alph")
      .out(plainBody[BigDecimal])
      .description("Get the OXM circulating supply")

  val getTotalSupply: BaseEndpoint[Unit, BigDecimal] =
    supplyEndpoint.get
      .in("total-alph")
      .out(plainBody[BigDecimal])
      .description("Get the OXM total supply")

  val getReservedSupply: BaseEndpoint[Unit, BigDecimal] =
    supplyEndpoint.get
      .in("reserved-alph")
      .out(plainBody[BigDecimal])
      .description("Get the OXM reserved supply")

  val getLockedSupply: BaseEndpoint[Unit, BigDecimal] =
    supplyEndpoint.get
      .in("locked-alph")
      .out(plainBody[BigDecimal])
      .description("Get the OXM locked supply")

  val getHeights: BaseEndpoint[Unit, ArraySeq[PerChainHeight]] =
    infosEndpoint.get
      .in("heights")
      .out(jsonBody[ArraySeq[PerChainHeight]])
      .description("List latest height for each chain")

  val getTotalTransactions: BaseEndpoint[Unit, Int] =
    infosEndpoint.get
      .in("total-transactions")
      .out(plainBody[Int])
      .description("Get the total number of transactions")

  val getAverageBlockTime: BaseEndpoint[Unit, ArraySeq[PerChainDuration]] =
    infosEndpoint.get
      .in("average-block-times")
      .out(jsonBody[ArraySeq[PerChainDuration]])
      .description("Get the average block time for each chain")
}
