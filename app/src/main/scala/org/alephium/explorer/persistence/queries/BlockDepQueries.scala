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

package org.alephium.explorer.persistence.queries

import slick.jdbc.{PositionedParameters, SetParameter, SQLActionBuilder}

import org.alephium.explorer.persistence.DBActionW
import org.alephium.explorer.persistence.model.BlockDepEntity
import org.alephium.explorer.persistence.schema.CustomSetParameter._

object BlockDepQueries {

  /**
    * Insert block_deps or ignore if there is a primary key conflict.
    *
    * Slick creates the following `INSERT` using string interpolation. Here
    * the same is achieved by manually creating the [[slick.jdbc.SQLActionBuilder]] so
    * our inserts can write multiple rows within a single `INSERT` statement.
    *
    * <a href="https://scala-slick.org/doc/3.3.3/sql.html#splicing-literal-values">Splicing</a>
    * is not used to insert values so these queries are still cacheable prepared-statements.
    */
  def insertBlockDeps(deps: Iterable[BlockDepEntity]): DBActionW[Int] =
    //generate '?' placeholders for the parameterised SQL query
    QuerySplitter.splitUpdates(rows = deps, columnsPerRow = 3) { (deps, placeholder) =>
      val query =
        s"""
           |INSERT INTO block_deps ("hash", "dep", "dep_order")
           |VALUES $placeholder
           |ON CONFLICT ON CONSTRAINT hash_deps_pk
           |    DO NOTHING
           |""".stripMargin

      //set parameters following the insert order defined by the query above
      val parameters: SetParameter[Unit] =
        (_: Unit, params: PositionedParameters) =>
          deps foreach { dep =>
            params >> dep.hash
            params >> dep.dep
            params >> dep.order
        }

      //Return builder generated by Slick's string interpolation
      SQLActionBuilder(
        queryParts = query,
        unitPConv  = parameters
      ).asUpdate
    }
}
