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

import org.scalamock.scalatest.MockFactory
import sttp.model.StatusCode

import org.oxygenium.api.ApiError
import org.oxygenium.explorer._
import org.oxygenium.explorer.ConfigDefaults.groupSetting
import org.oxygenium.explorer.HttpFixture._
import org.oxygenium.explorer.api.model.LogbackValue
import org.oxygenium.explorer.cache.{BlockCache, TestBlockCache, TransactionCache}
import org.oxygenium.explorer.config.BootMode
import org.oxygenium.explorer.persistence.{Database, DatabaseFixtureForAll}
import org.oxygenium.explorer.service._
import org.oxygenium.json.Json

@SuppressWarnings(Array("org.wartremover.warts.Var"))
class UtilsServerSpec()
    extends OxygeniumActorSpecLike
    with DatabaseFixtureForAll
    with HttpServerFixture
    with MockFactory {

  implicit val blockFlowClient: BlockFlowClient = mock[BlockFlowClient]
  implicit val blockCache: BlockCache           = TestBlockCache()
  implicit val transactionCache: TransactionCache = TransactionCache(
    new Database(BootMode.ReadWrite)
  )

  val utilsServer =
    new UtilsServer()

  val routes = utilsServer.routes

  "update global loglevel" in {
    List("TRACE", "DEBUG", "INFO", "WARN", "ERROR").foreach { level =>
      Put(s"/utils/update-global-loglevel", level) check { response =>
        response.code is StatusCode.Ok
      }
    }
    List("Trace", "debug", "yop").foreach { level =>
      Put(s"/utils/update-global-loglevel", level) check { response =>
        response.as[ApiError.BadRequest] is ApiError.BadRequest(
          s"""Invalid value for: body (expected value to be one of (TRACE, DEBUG, INFO, WARN, ERROR), but got: "$level")"""
        )
      }
    }
  }

  "update logback values" in {

    val logbackValues: Seq[LogbackValue] = Seq(
      LogbackValue("org.test", LogbackValue.Level.Debug),
      LogbackValue("yop", LogbackValue.Level.Trace)
    )

    Put(s"/utils/update-log-config", Json.write(logbackValues)) check { response =>
      response.code is StatusCode.Ok
    }

    val json = """
    [
      {
        "name": "foo",
        "level": "boo"
      }
    ]
    """

    Put(s"/utils/update-log-config", json) check { response =>
      response.as[ApiError.BadRequest] is ApiError.BadRequest(
        s"Invalid value for: body (Cannot decode level, expected one of: ArraySeq(TRACE, DEBUG, INFO, WARN, ERROR) at index 55: decoding failure)"
      )
    }
  }

  trait Fixture {}
}
