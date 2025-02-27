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

package org.oxygenium.tools

import com.typesafe.config.ConfigFactory

import org.oxygenium.api.OpenAPIWriters.openApiJson
import org.oxygenium.explorer.config._
import org.oxygenium.explorer.docs.Documentation
import org.oxygenium.util.{discard, Duration}

object OpenApiUpdate {
  def main(args: Array[String]): Unit = {
    discard {
      new Documentation {

        private val typesafeConfig         = ConfigFactory.load()
        private val config: ExplorerConfig = ExplorerConfig.load(typesafeConfig)

        val groupNum                           = config.groupNum
        val maxTimeIntervalExportTxs: Duration = config.maxTimeInterval.exportTxs
        val currencies                         = config.market.currencies

        private val json = openApiJson(docs, dropAuth = false, truncateAddresses = true)

        import java.io.PrintWriter
        new PrintWriter("../app/src/main/resources/explorer-backend-openapi.json") {
          write(json)
          write('\n')
          close
        }
      }
    }
  }
}
