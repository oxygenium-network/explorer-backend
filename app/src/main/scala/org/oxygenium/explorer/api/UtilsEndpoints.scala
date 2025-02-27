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
import org.oxygenium.explorer.api.model.LogbackValue
import org.oxygenium.explorer.persistence.queries.ExplainResult

// scalastyle:off magic.number
trait UtilsEndpoints extends BaseEndpoint with QueryParams {

  private val utilsEndpoint =
    baseEndpoint
      .tag("Utils")
      .in("utils")

  private val logLevels    = List("TRACE", "DEBUG", "INFO", "WARN", "ERROR")
  private val logLevelsStr = logLevels.mkString(", ")

  val sanityCheck: BaseEndpoint[Unit, Unit] =
    utilsEndpoint.put
      .in("sanity-check")
      .description("Perform a sanity check")

  val indexCheck: BaseEndpoint[Unit, ArraySeq[ExplainResult]] =
    utilsEndpoint.get
      .in("index-check")
      .out(jsonBody[ArraySeq[ExplainResult]])
      .description("Perform index check")

  val changeGlobalLogLevel: BaseEndpoint[String, Unit] =
    utilsEndpoint.put
      .in("update-global-loglevel")
      .in(plainBody[String].validate(Validator.enumeration(logLevels)))
      .description(s"Update global log level, accepted: $logLevelsStr")

  val changeLogConfig: BaseEndpoint[ArraySeq[LogbackValue], Unit] =
    utilsEndpoint.put
      .in("update-log-config")
      .in(jsonBody[ArraySeq[LogbackValue]])
      .description("Update logback values")
}
