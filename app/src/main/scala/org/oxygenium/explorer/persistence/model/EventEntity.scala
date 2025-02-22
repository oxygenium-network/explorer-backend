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

package org.oxygenium.explorer.persistence.model

import scala.collection.immutable.ArraySeq

import org.oxygenium.api.model.Val
import org.oxygenium.explorer.api.model.Event
import org.oxygenium.protocol.model.{Address, BlockHash, TransactionId}
import org.oxygenium.util.TimeStamp

@SuppressWarnings(
  Array("org.wartremover.warts.ArrayEquals")
) // Wartremover is complaining, don't now why :/
final case class EventEntity(
    blockHash: BlockHash,
    txHash: TransactionId,
    contractAddress: Address,
    inputAddress: Option[Address],
    timestamp: TimeStamp,
    eventIndex: Int,
    fields: ArraySeq[Val],
    eventOrder: Int
) {
  def toApi: Event = Event(
    blockHash,
    timestamp,
    txHash,
    contractAddress,
    inputAddress,
    eventIndex,
    fields
  )
}

object EventEntity {
  def from(
      blockHash: BlockHash,
      txHash: TransactionId,
      contractAddress: Address,
      inputAddress: Option[Address],
      timestamp: TimeStamp,
      eventIndex: Int,
      fields: ArraySeq[Val],
      order: Int
  ): EventEntity = {
    EventEntity(
      blockHash,
      txHash,
      contractAddress,
      inputAddress,
      timestamp,
      eventIndex,
      fields,
      order
    )
  }
}
