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
import sttp.model.StatusCode

import org.oxygenium.api.ApiError
import org.oxygenium.api.model.TimeInterval
import org.oxygenium.explorer.api.ChartsEndpoints
import org.oxygenium.explorer.api.model.{IntervalType, TimedCount}
import org.oxygenium.explorer.config.ExplorerConfig
import org.oxygenium.explorer.service.{HashrateService, TransactionHistoryService}

class ChartsServer(
    maxTimeInterval: ExplorerConfig.MaxTimeInterval
)(implicit
    val executionContext: ExecutionContext,
    dc: DatabaseConfig[PostgresProfile]
) extends Server
    with ChartsEndpoints {

  val routes: ArraySeq[Router => Route] =
    ArraySeq(
      route(getHashrates.serverLogic[Future] { case (timeInterval, interval) =>
        validateTimeInterval(timeInterval, interval) {
          HashrateService.get(timeInterval.from, timeInterval.to, interval)
        }
      }),
      route(getAllChainsTxCount.serverLogic[Future] { case (timeInterval, interval) =>
        validateTimeInterval(timeInterval, interval) {
          TransactionHistoryService
            .getAllChains(timeInterval.from, timeInterval.to, interval)
            .map { seq =>
              seq.map { case (timestamp, count) =>
                TimedCount(timestamp, count)
              }
            }
        }
      }),
      route(getPerChainTxCount.serverLogic[Future] { case (timeInterval, interval) =>
        validateTimeInterval(timeInterval, interval) {
          TransactionHistoryService
            .getPerChain(timeInterval.from, timeInterval.to, interval)
        }
      })
    )

  private def validateTimeInterval[A](timeInterval: TimeInterval, intervalType: IntervalType)(
      contd: => Future[A]
  ): Future[Either[ApiError[_ <: StatusCode], A]] =
    IntervalType.validateTimeInterval(
      timeInterval,
      intervalType,
      maxTimeInterval.hourly,
      maxTimeInterval.daily,
      maxTimeInterval.weekly
    )(contd)
}
