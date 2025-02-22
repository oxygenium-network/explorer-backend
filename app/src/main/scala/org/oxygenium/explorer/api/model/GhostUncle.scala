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

import org.oxygenium.api.model.GhostUncleBlockEntry
import org.oxygenium.explorer.api.Codecs._
import org.oxygenium.json.Json._
import org.oxygenium.protocol.model.{Address, BlockHash}

final case class GhostUncle(blockHash: BlockHash, miner: Address.Asset) {
  def toProtocol(): GhostUncleBlockEntry = GhostUncleBlockEntry(blockHash, miner)
}

object GhostUncle {
  implicit val codec: ReadWriter[GhostUncle] = macroRW
}
