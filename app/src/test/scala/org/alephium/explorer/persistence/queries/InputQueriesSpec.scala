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

package org.oxygenium.explorer.persistence.queries

import org.scalacheck.Gen
import slick.jdbc.PostgresProfile.api._

import org.oxygenium.explorer.AlephiumFutureSpec
import org.oxygenium.explorer.GenDBModel._
import org.oxygenium.explorer.persistence.{DatabaseFixtureForEach, DBRunner}
import org.oxygenium.explorer.persistence.model.InputEntity
import org.oxygenium.explorer.persistence.queries.InputQueries._
import org.oxygenium.explorer.persistence.queries.result.{InputsFromTxQR, InputsQR}
import org.oxygenium.explorer.persistence.schema.InputSchema

class InputQueriesSpec extends AlephiumFutureSpec with DatabaseFixtureForEach with DBRunner {

  "insertInputs" should {
    "insert and ignore inputs" in {

      def runTest(existingAndUpdated: Seq[(InputEntity, InputEntity)]) = {
        // fresh table
        run(InputSchema.table.delete).futureValue

        val existing = existingAndUpdated.map(_._1) // existing inputs
        val ignored  = existingAndUpdated.map(_._2) // updated inputs

        // insert existing
        run(insertInputs(existing)).futureValue
        run(InputSchema.table.result).futureValue should contain allElementsOf existing

        // insert should ignore existing inputs
        run(insertInputs(ignored)).futureValue is 0
        run(InputSchema.table.result).futureValue should contain allElementsOf existing
      }

      info("Test with random data size generated by ScalaCheck")
      forAll(Gen.listOf(updatedInputEntityGen()))(runTest)

      /** Following two test insert larger queries to test maximum number of parameters allowed by
        * Postgres per query i.e. [[Short.MaxValue]].
        *
        * See <a href="https://github.com/oxygenium/explorer-backend/issues/160">#160</a>
        */
      info(s"Large: Test with fixed '${Short.MaxValue}' data size")
      Gen
        .listOfN(Short.MaxValue, updatedInputEntityGen())
        .sample
        .foreach(runTest)

      info(s"Large: Test with fixed '${Short.MaxValue + 1}' data size")
      Gen
        .listOfN(Short.MaxValue + 1, updatedInputEntityGen())
        .sample
        .foreach(runTest)
    }
  }

  "inputsFromTxs" should {
    "read from inputs table" when {
      "empty" in {
        // clear table
        run(InputSchema.table.delete).futureValue
        run(InputSchema.table.length.result).futureValue is 0

        forAll(Gen.listOf(inputEntityGen())) { inputs =>
          // run query
          val hashes = inputs.map(input => (input.txHash, input.blockHash))
          val actual = run(InputQueries.inputsFromTxs(hashes)).futureValue

          // query output size is 0
          actual.size is 0
        }
      }

      "non-empty" in {
        forAll(Gen.listOf(inputEntityGen())) { inputs =>
          // persist test-data
          run(InputSchema.table.delete).futureValue
          run(InputSchema.table ++= inputs).futureValue

          // run query
          val hashes = inputs.map(input => (input.txHash, input.blockHash))
          val actual = run(InputQueries.inputsFromTxs(hashes)).futureValue

          // expected query result
          val expected =
            inputs.map { entity =>
              InputsFromTxQR(
                txHash = entity.txHash,
                inputOrder = entity.inputOrder,
                hint = entity.hint,
                outputRefKey = entity.outputRefKey,
                unlockScript = entity.unlockScript,
                outputRefTxHash = entity.outputRefTxHash,
                outputRefAddress = entity.outputRefAddress,
                outputRefAmount = entity.outputRefAmount,
                outputRefTokens = entity.outputRefTokens,
                contractInput = entity.contractInput
              )
            }

          actual should contain theSameElementsAs expected
        }
      }
    }
  }

  "getInputsQuery" should {
    "read inputs table" when {
      "empty" in {
        // table is empty
        run(InputSchema.table.length.result).futureValue is 0

        forAll(inputEntityGen()) { input =>
          // run query
          val actual =
            run(InputQueries.getInputsQuery(input.txHash, input.blockHash)).futureValue

          // query output size is 0
          actual.size is 0
        }
      }
    }

    "non-empty" in {
      forAll(Gen.listOf(inputEntityGen())) { inputs =>
        // no-need to clear the table for each iteration.
        run(InputSchema.table ++= inputs).futureValue

        // run query for each input
        inputs foreach { input =>
          val actual =
            run(InputQueries.getInputsQuery(input.txHash, input.blockHash)).futureValue

          // expected query result
          val expected: InputsQR =
            InputsQR(
              hint = input.hint,
              outputRefKey = input.outputRefKey,
              unlockScript = input.unlockScript,
              outputRefTxHash = input.outputRefTxHash,
              outputRefAddress = input.outputRefAddress,
              outputRefAmount = input.outputRefAmount,
              outputRefTokens = input.outputRefTokens,
              contractInput = input.contractInput
            )

          actual.toList should contain only expected
        }
      }
    }
  }

  "getMainChainInputs" should {
    "return main_chain InputEntities in order" in {
      forAll(Gen.listOf(inputEntityGen())) { inputs =>
        run(InputSchema.table.delete).futureValue
        run(InputSchema.table ++= inputs).futureValue

        val expected = inputs.filter(_.mainChain).sortBy(_.timestamp)

        // Ascending order
        locally {
          val actual = run(InputQueries.getMainChainInputs(true)).futureValue
          actual should contain inOrderElementsOf expected
        }

        // Descending order
        locally {
          val expectedReversed = expected.reverse
          val actual           = run(InputQueries.getMainChainInputs(false)).futureValue
          actual should contain inOrderElementsOf expectedReversed
        }
      }
    }
  }
}
