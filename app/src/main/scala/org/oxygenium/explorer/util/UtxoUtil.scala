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

package org.oxygenium.explorer.util

import java.math.BigInteger

import scala.collection.immutable.ArraySeq

import org.oxygenium.explorer.api.model.{Input, Output}
import org.oxygenium.protocol.model.Address
import org.oxygenium.util.U256

object UtxoUtil {
  def amountForAddressInInputs(address: Address, inputs: ArraySeq[Input]): Option[U256] = {
    inputs
      .filter(_.address == Some(address))
      .map(_.attoOxmAmount)
      .collect { case Some(amount) => amount }
      .foldLeft(Option(U256.Zero)) { case (acc, amount) => acc.flatMap(_.add(amount)) }
  }

  def amountForAddressInOutputs(address: Address, outputs: ArraySeq[Output]): Option[U256] = {
    outputs
      .filter(_.address == address)
      .map(_.attoOxmAmount)
      .foldLeft(Option(U256.Zero)) { case (acc, amount) => acc.flatMap(_.add(amount)) }
  }

  def deltaAmountForAddress(
      address: Address,
      inputs: ArraySeq[Input],
      outputs: ArraySeq[Output]
  ): Option[BigInteger] = {
    for {
      in  <- amountForAddressInInputs(address, inputs)
      out <- amountForAddressInOutputs(address, outputs)
    } yield {
      out.v.subtract(in.v)
    }
  }

  @SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
  def fromAddresses(inputs: ArraySeq[Input]): ArraySeq[Address] = {
    inputs.collect { case input if input.address.isDefined => input.address.get }.distinct
  }

  def toAddressesWithoutChangeAddresses(
      outputs: ArraySeq[Output],
      changeAddresses: ArraySeq[Address]
  ): ArraySeq[Address] = {
    outputs.collect {
      case output if !changeAddresses.contains(output.address) => output.address
    }.distinct
  }
}
