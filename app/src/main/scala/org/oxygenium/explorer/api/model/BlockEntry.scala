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

import java.math.BigInteger

import scala.collection.immutable.ArraySeq

import akka.util.ByteString

import org.oxygenium.api.UtilJson._
import org.oxygenium.explorer.api.Codecs._
import org.oxygenium.explorer.api.Json.groupIndexReadWriter
import org.oxygenium.explorer.service.FlowEntity
import org.oxygenium.json.Json._
import org.oxygenium.protocol.Hash
import org.oxygenium.protocol.model.{BlockHash, GroupIndex}
import org.oxygenium.util.{AVector, TimeStamp}

final case class BlockEntry(
    hash: BlockHash,
    timestamp: TimeStamp,
    chainFrom: GroupIndex,
    chainTo: GroupIndex,
    height: Height,
    deps: ArraySeq[BlockHash],
    nonce: ByteString,
    version: Byte,
    depStateHash: Hash,
    txsHash: Hash,
    txNumber: Int,
    target: ByteString,
    hashRate: BigInteger,
    parent: Option[BlockHash],
    mainChain: Boolean,
    ghostUncles: ArraySeq[GhostUncle]
) extends FlowEntity {

  def toProtocol(
      transactions: ArraySeq[Transaction]
  ): org.oxygenium.api.model.BlockEntry = {
    org.oxygenium.api.model.BlockEntry(
      hash,
      timestamp,
      chainFrom.value,
      chainTo.value,
      height.value,
      AVector.from(deps),
      AVector.from(transactions.map(_.toProtocol())),
      nonce,
      version,
      depStateHash,
      txsHash,
      target,
      AVector.from(ghostUncles.map(_.toProtocol()))
    )
  }

}

object BlockEntry {
  implicit val codec: ReadWriter[BlockEntry] = macroRW
}

final case class BlockEntryLite(
    hash: BlockHash,
    timestamp: TimeStamp,
    chainFrom: GroupIndex,
    chainTo: GroupIndex,
    height: Height,
    txNumber: Int,
    mainChain: Boolean,
    hashRate: BigInteger
)
object BlockEntryLite {
  implicit val codec: ReadWriter[BlockEntryLite] = macroRW
}
