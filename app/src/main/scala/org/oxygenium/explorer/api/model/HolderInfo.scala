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

import org.oxygenium.explorer.api.Json._
import org.oxygenium.json.Json._
import org.oxygenium.protocol.model.Address
import org.oxygenium.util.U256

final case class HolderInfo(address: Address, balance: U256)

object HolderInfo {
  implicit val readWriter: ReadWriter[HolderInfo] = macroRW
}
