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

import java.time.Instant

import scala.collection.immutable.ArraySeq

import akka.util.ByteString
import sttp.tapir.Schema

import org.oxygenium.api.TapirSchemas._
import org.oxygenium.api.UtilJson._
import org.oxygenium.explorer.api.Json._
import org.oxygenium.explorer.util.UtxoUtil
import org.oxygenium.json.Json._
import org.oxygenium.protocol.OXM
import org.oxygenium.protocol.model.{BlockHash, TransactionId}
import org.oxygenium.protocol.model.Address
import org.oxygenium.util.{TimeStamp, U256}
import org.oxygenium.util.AVector

final case class Transaction(
    hash: TransactionId,
    blockHash: BlockHash,
    timestamp: TimeStamp,
    inputs: ArraySeq[Input],
    outputs: ArraySeq[Output],
    version: Byte,
    networkId: Byte,
    scriptOpt: Option[String],
    gasAmount: Int,
    gasPrice: U256,
    scriptExecutionOk: Boolean,
    inputSignatures: ArraySeq[ByteString],
    scriptSignatures: ArraySeq[ByteString],
    coinbase: Boolean
) {
  def toCsv(address: Address): String = {
    val dateTime         = Instant.ofEpochMilli(timestamp.millis)
    val fromAddresses    = UtxoUtil.fromAddresses(inputs)
    val fromAddressesStr = fromAddresses.mkString("-")
    val toAddresses =
      UtxoUtil.toAddressesWithoutChangeAddresses(outputs, fromAddresses).mkString("-")
    val deltaAmount = UtxoUtil.deltaAmountForAddress(address, inputs, outputs)
    val amount      = deltaAmount.map(_.toString).getOrElse("")
    val amountHint = deltaAmount
      .map(delta =>
        new java.math.BigDecimal(delta).divide(new java.math.BigDecimal(OXM.oneOxm.v))
      )
      .map(_.toString)
      .getOrElse("")
    s"${hash.toHexString},${blockHash.toHexString},${timestamp.millis},$dateTime,$fromAddressesStr,$toAddresses,$amount,$amountHint\n"
  }

  def toProtocol(): org.oxygenium.api.model.Transaction = {
    val (inputContracts, inputAssets)    = inputs.partition(_.contractInput)
    val (fixedOutputs, generatedOutputs) = outputs.partition(_.fixedOutput)
    val unsigned: org.oxygenium.api.model.UnsignedTx = org.oxygenium.api.model.UnsignedTx(
      txId = hash,
      version = version,
      networkId = networkId,
      scriptOpt = scriptOpt.map(org.oxygenium.api.model.Script.apply),
      gasAmount = gasAmount,
      gasPrice = gasPrice,
      inputs = AVector.from(inputAssets.map(_.toProtocol())),
      fixedOutputs = AVector.from(fixedOutputs.flatMap(Output.toFixedAssetOutput))
    )
    org.oxygenium.api.model.Transaction(
      unsigned = unsigned,
      scriptExecutionOk = scriptExecutionOk,
      contractInputs = AVector.from(inputContracts.map(_.outputRef.toProtocol())),
      generatedOutputs = AVector.from(generatedOutputs.flatMap(Output.toProtocol)),
      inputSignatures = AVector.from(inputSignatures),
      scriptSignatures = AVector.from(scriptSignatures)
    )
  }
}

object Transaction {
  implicit val txRW: ReadWriter[Transaction] = macroRW

  val csvHeader: String =
    "hash,blockHash,unixTimestamp,dateTimeUTC,fromAddresses,toAddresses,amount,hintAmount\n"

  implicit val schema: Schema[Transaction] = Schema.derived[Transaction]
}
