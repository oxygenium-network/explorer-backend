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

package org.oxygenium.explorer.api.model

import sttp.tapir.Schema

import org.oxygenium.api.TapirSchemas._
import org.oxygenium.explorer.api.Json._
import org.oxygenium.json.Json._
import org.oxygenium.protocol.Hash

final case class OutputRef(hint: Int, key: Hash) {

  def toProtocol(): org.oxygenium.api.model.OutputRef =
    org.oxygenium.api.model.OutputRef(hint, key)
}

object OutputRef {
  implicit val readWriter: ReadWriter[OutputRef] = macroRW
  implicit val schema: Schema[OutputRef]         = Schema.derived[OutputRef]
}
