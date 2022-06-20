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

package org.alephium.explorer.cache

import scala.collection.immutable.ArraySeq
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

import slick.basic.DatabaseConfig
import slick.jdbc.PostgresProfile
import slick.jdbc.PostgresProfile.api._

import org.alephium.explorer.persistence.DBRunner._
import org.alephium.explorer.persistence.queries.TransactionQueries
import org.alephium.util.Service

object TransactionCache {

  @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
  def apply(reloadAfter: FiniteDuration = 5.seconds)(
      implicit ec: ExecutionContext,
      dc: DatabaseConfig[PostgresProfile]): TransactionCache =
    new TransactionCache(reloadAfter)
}

/**
  * Transaction related cache
  *
  * @param mainChainTxnCount Stores current total number of `main_chain` transaction.
  */
class TransactionCache(reloadAfter: FiniteDuration)(implicit val executionContext: ExecutionContext,
                                                    dc: DatabaseConfig[PostgresProfile])
    extends Service {

  private val mainChainTxnCount: AsyncReloadingCache[Int] = {
    AsyncReloadingCache(0, reloadAfter) { _ =>
      run(TransactionQueries.mainTransactions.length.result)
    }
  }

  def getMainChainTxnCount(): Int =
    mainChainTxnCount.get()

  override def startSelfOnce(): Future[Unit] = {
    mainChainTxnCount.expireAndReloadFuture().map(_ => ())
  }

  override def stopSelfOnce(): Future[Unit] = {
    Future.unit
  }

  override def subServices: ArraySeq[Service] = ArraySeq.empty
}
