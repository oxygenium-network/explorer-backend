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

package org.oxygenium.explorer

import scala.collection.immutable.ArraySeq
import scala.concurrent.{ExecutionContext, Future}

import com.typesafe.scalalogging.StrictLogging
import slick.basic.DatabaseConfig
import slick.jdbc.PostgresProfile

import org.oxygenium.explorer.cache.{BlockCache, MetricCache, TransactionCache}
import org.oxygenium.explorer.config.{BootMode, ExplorerConfig}
import org.oxygenium.explorer.persistence.Database
import org.oxygenium.explorer.service._
import org.oxygenium.explorer.util.Scheduler
import org.oxygenium.util.Service

/** Boot-up states for Explorer: Explorer can be started in the following three states
  *
  *   - ReadOnly: [[org.oxygenium.explorer.ExplorerState.ReadOnly]]
  *   - ReadWrite: [[org.oxygenium.explorer.ExplorerState.ReadWrite]]
  *   - WriteOnly: [[org.oxygenium.explorer.ExplorerState.WriteOnly]]
  */
sealed trait ExplorerState extends Service with StrictLogging {
  implicit def config: ExplorerConfig
  implicit def databaseConfig: DatabaseConfig[PostgresProfile]

  implicit lazy val groupSettings: GroupSetting =
    GroupSetting(config.groupNum)

  lazy val database: Database =
    new Database(config.bootMode)(executionContext, databaseConfig)

  implicit lazy val blockCache: BlockCache =
    BlockCache(
      config.cacheRowCountReloadPeriod,
      config.cacheBlockTimesReloadPeriod,
      config.cacheLatestBlocksReloadPeriod
    )(groupSettings, executionContext, database.databaseConfig)

  implicit lazy val blockFlowClient: BlockFlowClient =
    BlockFlowClient(
      uri = config.blockFlowUri,
      groupNum = config.groupNum,
      maybeApiKey = config.maybeBlockFlowApiKey,
      directCliqueAccess = config.directCliqueAccess
    )

  override def startSelfOnce(): Future[Unit] = {
    Future.unit
  }

  override def stopSelfOnce(): Future[Unit] = {
    Future.unit
  }

}

sealed trait ExplorerStateRead extends ExplorerState {

  implicit lazy val metricCache: MetricCache =
    new MetricCache(
      database,
      config.cacheMetricsReloadPeriod
    )

  lazy val transactionCache: TransactionCache =
    TransactionCache(database)(executionContext)

  lazy val marketService: MarketService.Impl = MarketService.Impl.default(config.market)

  private lazy val routes =
    AppServer
      .routes(
        marketService,
        config.exportTxsNumberThreshold,
        config.streamParallelism,
        config.maxTimeInterval,
        config.market
      )(
        executionContext,
        database.databaseConfig,
        blockFlowClient,
        blockCache,
        metricCache,
        transactionCache,
        groupSettings
      )
  lazy val httpServer: ExplorerHttpServer =
    new ExplorerHttpServer(
      config.host,
      config.port,
      routes,
      database
    )
}

sealed trait ExplorerStateWrite extends ExplorerState {

  // See issue #356
  implicit private val scheduler: Scheduler = Scheduler("SYNC_SERVICES")

  override def startSelfOnce(): Future[Unit] = {
    SyncServices.startSyncServices(config)
  }
}

object ExplorerState {

  def apply(mode: BootMode)(implicit
      config: ExplorerConfig,
      databaseConfig: DatabaseConfig[PostgresProfile],
      executionContext: ExecutionContext
  ): ExplorerState =
    mode match {
      case BootMode.ReadOnly  => ExplorerState.ReadOnly()
      case BootMode.ReadWrite => ExplorerState.ReadWrite()
      case BootMode.WriteOnly => ExplorerState.WriteOnly()
    }

  /** State of Explorer is started in read-only mode */
  final case class ReadOnly()(implicit
      val config: ExplorerConfig,
      val databaseConfig: DatabaseConfig[PostgresProfile],
      val executionContext: ExecutionContext
  ) extends ExplorerStateRead {

    override def subServices: ArraySeq[Service] =
      ArraySeq(
        httpServer,
        marketService,
        metricCache,
        transactionCache,
        database
      )
  }

  /** State of Explorer is started in read-write mode */
  final case class ReadWrite()(implicit
      val config: ExplorerConfig,
      val databaseConfig: DatabaseConfig[PostgresProfile],
      val executionContext: ExecutionContext
  ) extends ExplorerStateRead
      with ExplorerStateWrite {

    override def subServices: ArraySeq[Service] =
      ArraySeq(
        httpServer,
        marketService,
        metricCache,
        transactionCache,
        database,
        blockFlowClient
      )
  }

  /** State of Explorer is started in Sync only mode */
  final case class WriteOnly()(implicit
      val config: ExplorerConfig,
      val databaseConfig: DatabaseConfig[PostgresProfile],
      val executionContext: ExecutionContext
  ) extends ExplorerStateWrite {

    override def subServices: ArraySeq[Service] =
      ArraySeq(
        blockFlowClient,
        database
      )
  }
}
