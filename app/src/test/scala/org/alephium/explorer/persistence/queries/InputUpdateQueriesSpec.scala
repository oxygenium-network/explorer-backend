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

package org.oxygenium.explorer.persistence.queries

import slick.jdbc.PostgresProfile.api._

import org.oxygenium.explorer.OxygeniumFutureSpec
import org.oxygenium.explorer.GenDBModel._
import org.oxygenium.explorer.persistence.{DatabaseFixtureForEach, DBRunner}
import org.oxygenium.explorer.persistence.schema.{InputSchema, OutputSchema}
import org.oxygenium.explorer.persistence.schema.CustomJdbcTypes._

class InputUpdateQueriesSpec extends OxygeniumFutureSpec with DatabaseFixtureForEach with DBRunner {

  "Input Update" should {
    "update inputs when address is already set" in {
      forAll(outputEntityGen, inputEntityGen()) { case (output, input) =>
        run(for {
          _ <- OutputSchema.table += output
          _ <- InputSchema.table +=
            input.copy(outputRefKey = output.key, outputRefAddress = Some(output.address))
        } yield ()).futureValue

        val inputBeforeUpdate =
          run(InputSchema.table.filter(_.outputRefKey === output.key).result.head).futureValue

        inputBeforeUpdate.outputRefAddress is Some(output.address)
        inputBeforeUpdate.outputRefAmount is None

        run(InputUpdateQueries.updateInputs()).futureValue

        val updatedInput =
          run(InputSchema.table.filter(_.outputRefKey === output.key).result.head).futureValue

        updatedInput.outputRefAddress is Some(output.address)
        updatedInput.outputRefAmount is Some(output.amount)
      }
    }

    "update inputs when address is not set" in {
      forAll(outputEntityGen, inputEntityGen()) { case (output, input) =>
        run(for {
          _ <- OutputSchema.table += output
          _ <- InputSchema.table +=
            input.copy(outputRefKey = output.key)
        } yield ()).futureValue

        run(InputUpdateQueries.updateInputs()).futureValue

        val updatedInput =
          run(InputSchema.table.filter(_.outputRefKey === output.key).result.head).futureValue

        updatedInput.outputRefAddress is Some(output.address)
        updatedInput.outputRefAmount is Some(output.amount)
      }
    }
  }
}
