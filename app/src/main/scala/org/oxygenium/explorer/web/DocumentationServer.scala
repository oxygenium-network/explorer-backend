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

import io.vertx.ext.web._

import org.oxygenium.api.OpenAPIWriters.openApiJson
import org.oxygenium.explorer.GroupSetting
import org.oxygenium.explorer.docs.Documentation
import org.oxygenium.http.SwaggerUI
import org.oxygenium.util.Duration

class DocumentationServer(
    val maxTimeIntervalExportTxs: Duration,
    val currencies: ArraySeq[String]
)(implicit
    groupSetting: GroupSetting
) extends Server
    with Documentation {

  val groupNum = groupSetting.groupNum

  val routes: ArraySeq[Router => Route] =
    ArraySeq.from(
      SwaggerUI(
        openApiJson(docs, dropAuth = false, truncateAddresses = true),
        openapiFileName = "explorer-backend-openapi.json"
      ).map(route(_))
    )
}
