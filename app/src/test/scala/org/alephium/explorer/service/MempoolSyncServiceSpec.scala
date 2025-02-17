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

import scala.collection.immutable.ArraySeq
import scala.concurrent.Future
import scala.concurrent.duration._

import org.scalacheck.Gen
import sttp.model.Uri

import org.oxygenium.explorer.AlephiumFutureSpec
import org.oxygenium.explorer.GenApiModel.mempooltransactionGen
import org.oxygenium.explorer.api.model.MempoolTransaction
import org.oxygenium.explorer.persistence.DatabaseFixtureForEach
import org.oxygenium.explorer.persistence.dao.MempoolDao
import org.oxygenium.explorer.util.Scheduler
import org.oxygenium.explorer.util.TestUtils._

class MempoolSyncServiceSpec extends AlephiumFutureSpec with DatabaseFixtureForEach {

  "start/sync/stop" in new Fixture {
    using(Scheduler("test")) { implicit scheduler =>
      MempoolSyncService.start(ArraySeq(Uri("")), 100.milliseconds)

      MempoolDao.listHashes().futureValue is ArraySeq.empty

      mempoolTransactions = Gen.listOfN(10, mempooltransactionGen).sample.get

      eventually {
        MempoolDao.listHashes().futureValue.toSet is mempoolTransactions.map(_.hash).toSet
      }

      val head   = mempoolTransactions.head
      val last   = mempoolTransactions.last
      val middle = mempoolTransactions(5)

      val newMempoolTransactions =
        mempoolTransactions.filterNot(tx => tx == head || tx == last || tx == middle)

      mempoolTransactions = newMempoolTransactions

      eventually {
        MempoolDao.listHashes().futureValue.toSet is newMempoolTransactions
          .map(_.hash)
          .toSet
      }
    }
  }

  trait Fixture {
    var mempoolTransactions: ArraySeq[MempoolTransaction] = ArraySeq.empty

    implicit val blockFlowClient: BlockFlowClient = new EmptyBlockFlowClient {
      override def fetchMempoolTransactions(uri: Uri): Future[ArraySeq[MempoolTransaction]] =
        Future.successful(mempoolTransactions)
    }
  }
}
