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

package org.oxygenium.explorer.service

import slick.jdbc.PostgresProfile.api._

import org.oxygenium.explorer.AlephiumFutureSpec
import org.oxygenium.explorer.GenDBModel._
import org.oxygenium.explorer.persistence.{DatabaseFixtureForEach, DBRunner}
import org.oxygenium.explorer.persistence.model.{BlockHeader, InputEntity, LatestBlock}
import org.oxygenium.explorer.persistence.model.AppState.LastFinalizedInputTime
import org.oxygenium.explorer.persistence.queries.{AppStateQueries, BlockQueries, InputQueries}
import org.oxygenium.explorer.persistence.schema.LatestBlockSchema
import org.oxygenium.protocol.model.GroupIndex
import org.oxygenium.util.{Duration, TimeStamp}

class FinalizerServiceSpec extends AlephiumFutureSpec with DatabaseFixtureForEach with DBRunner {

  "getStartEndTime - return nothing if there's no input" in new Fixture {
    run(FinalizerService.getStartEndTime()).futureValue is None
  }

  "getStartEndTime - return nothing if there's only 1 input" in new Fixture {

    val input1 = input(TimeStamp.now())
    run(InputQueries.insertInputs(Seq(input1))).futureValue

    run(FinalizerService.getStartEndTime()).futureValue is None
  }

  "getStartEndTime - return nothing if all inputs are after finalization time" in new Fixture {

    val input1 = input(
      firstFinalizationTime.plusHoursUnsafe(1)
    )
    val input2 = input(
      firstFinalizationTime.plusHoursUnsafe(2)
    )

    run(InputQueries.insertInputs(Seq(input1, input2))).futureValue
    run(FinalizerService.getStartEndTime()).futureValue is None
  }

  "getStartEndTime - return correct finalization time" in new Fixture {

    val input1 = input(
      firstFinalizationTime.minusUnsafe(Duration.ofHoursUnsafe(10))
    )
    val blockHeader2 = block(firstFinalizationTime.minusUnsafe(Duration.ofHoursUnsafe(1)))

    run(BlockQueries.insertBlockHeaders(Seq(blockHeader2))).futureValue
    run(InputQueries.insertInputs(Seq(input1))).futureValue
    run(LatestBlockSchema.table += latestBlock(blockHeader2)).futureValue

    val Some((start1, end1)) = run(FinalizerService.getStartEndTime()).futureValue

    start1 is input1.timestamp
    end1 is blockHeader2.timestamp

    val input3 = input(TimeStamp.now())

    run(InputQueries.insertInputs(Seq(input3))).futureValue

    val Some((start2, end2)) = run(FinalizerService.getStartEndTime()).futureValue

    start2 is input1.timestamp

    firstFinalizationTime.isBefore(end2)
    end2.isBefore(FinalizerService.finalizationTime)

    run(AppStateQueries.insertOrUpdate(LastFinalizedInputTime(blockHeader2.timestamp))).futureValue

    val Some((start3, _)) = run(FinalizerService.getStartEndTime()).futureValue

    start3 is blockHeader2.timestamp
  }

  trait Fixture {
    // FinalizerService.finalizationTime is a function based on TimeStamp.now()
    val firstFinalizationTime = FinalizerService.finalizationTime

    def input(timestamp: TimeStamp): InputEntity =
      inputEntityGen().sample.get.copy(timestamp = timestamp, mainChain = true)

    def block(timestamp: TimeStamp): BlockHeader =
      blockHeaderGen.sample.get.copy(
        timestamp = timestamp,
        chainFrom = new GroupIndex(0),
        chainTo = new GroupIndex(0),
        mainChain = true
      )

    def latestBlock(block: BlockHeader): LatestBlock =
      LatestBlock(
        block.hash,
        block.timestamp,
        block.chainFrom,
        block.chainTo,
        block.height,
        block.target,
        block.hashrate
      )
  }
}
