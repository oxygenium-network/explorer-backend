// Copyright 2018 The Alephium Authors
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

import scala.collection.immutable.ArraySeq

import org.oxygenium.api.UtilJson._
import org.oxygenium.api.model.Val
import org.oxygenium.explorer.api.Codecs._
import org.oxygenium.json.Json._
import org.oxygenium.protocol.model.{Address, BlockHash, TransactionId}
import org.oxygenium.util.TimeStamp

final case class Event(
    blockHash: BlockHash,
    timestamp: TimeStamp,
    txHash: TransactionId,
    contractAddress: Address,
    inputAddress: Option[Address],
    eventIndex: Int,
    fields: ArraySeq[Val]
)

object Event {
  implicit val codec: ReadWriter[Event] = macroRW
}
