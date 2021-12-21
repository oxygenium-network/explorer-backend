// Copyright 2018 The Alephium Authors
// This file is part of the alephium project.
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

package org.alephium.explorer.persistence.schema

import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile
import slick.lifted.{Index, PrimaryKey, ProvenShape}

import org.alephium.explorer.api.model.BlockEntry

trait BlockDepsSchema extends CustomTypes {
  val config: DatabaseConfig[JdbcProfile]

  import config.profile.api._

  class BlockDeps(tag: Tag)
      extends Table[(BlockEntry.Hash, BlockEntry.Hash, Int)](tag, "block_deps") {
    def hash: Rep[BlockEntry.Hash] = column[BlockEntry.Hash]("hash", O.SqlType("BYTEA"))
    def dep: Rep[BlockEntry.Hash]  = column[BlockEntry.Hash]("dep", O.SqlType("BYTEA"))
    def order: Rep[Int]            = column[Int]("order")

    def pk: PrimaryKey = primaryKey("hash_deps_pk", (hash, dep))
    def hashIdx: Index = index("deps_hash_idx", hash)
    def depIdx: Index  = index("deps_dep_idx", dep)

    def * : ProvenShape[(BlockEntry.Hash, BlockEntry.Hash, Int)] = (hash, dep, order)
  }

  val blockDepsTable: TableQuery[BlockDeps] = TableQuery[BlockDeps]
}
