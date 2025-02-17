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

package org.oxygenium.explorer.persistence.queries.result

import scala.collection.immutable.ArraySeq

import akka.util.ByteString
import slick.jdbc.{GetResult, PositionedResult}

import org.oxygenium.explorer.api.model._
import org.oxygenium.explorer.persistence.model.{OutputEntity, OutputEntityLike}
import org.oxygenium.explorer.persistence.schema.CustomGetResult._
import org.oxygenium.protocol.Hash
import org.oxygenium.protocol.model.{Address, TransactionId}
import org.oxygenium.util.{TimeStamp, U256}

object OutputsQR {

  val selectFields: String =
    "output_type, hint, key, amount, address, tokens, lock_time, message, spent_finalized, fixed_output"

  implicit val outputsQRGetResult: GetResult[OutputsQR] =
    (result: PositionedResult) =>
      OutputsQR(
        outputType = result.<<,
        hint = result.<<,
        key = result.<<,
        amount = result.<<,
        address = result.<<,
        tokens = result.<<?,
        lockTime = result.<<?,
        message = result.<<?,
        spentFinalized = result.<<?,
        fixedOutput = result.<<
      )
}

/** Query result for [[org.oxygenium.explorer.persistence.queries.OutputQueries.getOutputsQuery]] */
final case class OutputsQR(
    outputType: OutputEntity.OutputType,
    hint: Int,
    key: Hash,
    amount: U256,
    address: Address,
    tokens: Option[ArraySeq[Token]],
    lockTime: Option[TimeStamp],
    message: Option[ByteString],
    spentFinalized: Option[TransactionId],
    fixedOutput: Boolean
) extends OutputEntityLike
