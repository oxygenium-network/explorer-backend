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

package org.oxygenium.explorer.persistence.schema

import slick.jdbc.PostgresProfile.api._
import slick.lifted.ProvenShape

import org.oxygenium.explorer.persistence.model.HolderEntity
import org.oxygenium.explorer.persistence.schema.CustomJdbcTypes._
import org.oxygenium.protocol.model.Address
import org.oxygenium.util.U256

object OxmHolderSchema extends SchemaMainChain[HolderEntity]("alph_holders") {

  class OxmHolders(tag: Tag) extends Table[HolderEntity](tag, name) {
    def address: Rep[Address] = column[Address]("address", O.PrimaryKey)
    def balance: Rep[U256] =
      column[U256]("balance", O.SqlType("DECIMAL(80,0)")) // U256.MaxValue has 78 digits

    def * : ProvenShape[HolderEntity] =
      (
        address,
        balance
      )
        .<>((HolderEntity.apply _).tupled, HolderEntity.unapply)
  }

  val table: TableQuery[OxmHolders] = TableQuery[OxmHolders]
}
