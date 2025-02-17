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

import akka.util.ByteString
import sttp.tapir.Schema

import org.oxygenium.api.TapirSchemas._
import org.oxygenium.api.UtilJson._
import org.oxygenium.explorer.api.Json._
import org.oxygenium.explorer.api.Schemas.configuration
import org.oxygenium.json.Json._
import org.oxygenium.protocol.Hash
import org.oxygenium.protocol.model.{Address, TransactionId}
import org.oxygenium.util.{TimeStamp, U256}
import org.oxygenium.util.AVector

sealed trait Output {
  def hint: Int
  def key: Hash
  def attoAlphAmount: U256
  def address: Address
  def tokens: Option[ArraySeq[Token]]
  def spent: Option[TransactionId]
  def fixedOutput: Boolean
}

@SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
@upickle.implicits.key("AssetOutput")
final case class AssetOutput(
    hint: Int,
    key: Hash,
    attoAlphAmount: U256,
    address: Address,
    tokens: Option[ArraySeq[Token]] = None,
    lockTime: Option[TimeStamp] = None,
    message: Option[ByteString] = None,
    spent: Option[TransactionId] = None,
    fixedOutput: Boolean
) extends Output {}

@SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
@upickle.implicits.key("ContractOutput")
final case class ContractOutput(
    hint: Int,
    key: Hash,
    attoAlphAmount: U256,
    address: Address,
    tokens: Option[ArraySeq[Token]] = None,
    spent: Option[TransactionId] = None,
    fixedOutput: Boolean
) extends Output

object Output {

  def toFixedAssetOutput(
      output: Output
  ): Option[org.oxygenium.api.model.FixedAssetOutput] = {
    output match {
      case asset: AssetOutput if asset.fixedOutput =>
        asset.address match {
          case assetAddress: Address.Asset =>
            val amount = org.oxygenium.api.model.Amount(asset.attoAlphAmount)
            Some(
              org.oxygenium.api.model.FixedAssetOutput(
                asset.hint,
                asset.key,
                amount,
                assetAddress,
                tokens = asset.tokens
                  .map(tokens => AVector.from(tokens.map(_.toProtocol())))
                  .getOrElse(AVector.empty),
                lockTime = asset.lockTime.getOrElse(TimeStamp.zero),
                asset.message.getOrElse(ByteString.empty)
              )
            )
          case _ => None
        }
      case _ => None
    }
  }

  def toProtocol(output: Output): Option[org.oxygenium.api.model.Output] =
    (output, output.address) match {
      case (asset: AssetOutput, assetAddress: Address.Asset) =>
        Some(
          org.oxygenium.api.model.AssetOutput(
            output.hint,
            output.key,
            org.oxygenium.api.model.Amount(output.attoAlphAmount),
            assetAddress,
            tokens =
              output.tokens.map(t => AVector.from(t.map(_.toProtocol()))).getOrElse(AVector.empty),
            lockTime = asset.lockTime.getOrElse(TimeStamp.zero),
            message = asset.message.getOrElse(ByteString.empty)
          )
        )
      case (_: ContractOutput, contractAddress: Address.Contract) =>
        Some(
          org.oxygenium.api.model.ContractOutput(
            output.hint,
            output.key,
            org.oxygenium.api.model.Amount(output.attoAlphAmount),
            contractAddress,
            tokens = output.tokens
              .map(tokens => AVector.from(tokens.map(_.toProtocol())))
              .getOrElse(AVector.empty)
          )
        )
      case _ => None
    }

  implicit val assetReadWriter: ReadWriter[AssetOutput]       = macroRW
  implicit val contractReadWriter: ReadWriter[ContractOutput] = macroRW

  implicit val outputReadWriter: ReadWriter[Output] =
    ReadWriter.merge(assetReadWriter, contractReadWriter)
  implicit val contractSchema: Schema[ContractOutput] = Schema.derived[ContractOutput]
  implicit val AssetSchema: Schema[AssetOutput]       = Schema.derived[AssetOutput]
  implicit val schema: Schema[Output]                 = Schema.derived[Output]
}
