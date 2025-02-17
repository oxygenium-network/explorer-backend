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

import sttp.tapir.Schema

import org.oxygenium.api.TapirSchemas._
import org.oxygenium.explorer.api.Json._
import org.oxygenium.explorer.api.Schemas._
import org.oxygenium.json.Json._
import org.oxygenium.protocol.model.TokenId
import org.oxygenium.serde._
import org.oxygenium.util.U256

final case class Token(id: TokenId, amount: U256) {
  def toProtocol(): org.oxygenium.api.model.Token = org.oxygenium.api.model.Token(id, amount)
}

object Token {
  implicit val readWriter: ReadWriter[Token] = macroRW
  implicit val serde: Serde[Token] =
    Serde.forProduct2(
      Token.apply,
      t => (t.id, t.amount)
    )
  implicit val schema: Schema[Token] = Schema.derived[Token]
}
