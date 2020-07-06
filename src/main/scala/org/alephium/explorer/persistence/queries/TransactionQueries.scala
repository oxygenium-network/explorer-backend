package org.alephium.explorer.persistence.queries

import scala.concurrent.ExecutionContext

import slick.basic.DatabaseConfig
import slick.dbio.DBIOAction
import slick.jdbc.JdbcProfile

import org.alephium.explorer.api.model.{Address, BlockEntry, Transaction}
import org.alephium.explorer.persistence.{DBActionR, DBActionW}
import org.alephium.explorer.persistence.model._
import org.alephium.explorer.persistence.schema.{InputSchema, OutputSchema, TransactionSchema}
import org.alephium.util.{AVector, TimeStamp}

trait TransactionQueries extends TransactionSchema with InputSchema with OutputSchema {

  implicit def executionContext: ExecutionContext
  val config: DatabaseConfig[JdbcProfile]
  import config.profile.api._

  def insertTransactionFromBlockQuery(blockEntity: BlockEntity): DBActionW[Unit] =
    ((transactionsTable ++= blockEntity.transactions.toArray) >>
      (inputsTable ++= blockEntity.inputs.toArray) >>
      (outputsTable ++= blockEntity.outputs.toArray))
      .map(_ => ())

  def listTransactionsAction(blockHash: BlockEntry.Hash): DBActionR[Seq[Transaction]] =
    transactionsTable
      .filter(_.blockHash === blockHash)
      .sortBy(_.timestamp.asc)
      .result
      .flatMap(hashes => DBIOAction.sequence(hashes.map(getKnownTransactionFromEntityAction)))

  def getTransactionAction(txHash: Transaction.Hash): DBActionR[Option[Transaction]] =
    transactionsTable.filter(_.hash === txHash).result.headOption.flatMap {
      case None     => DBIOAction.successful(None)
      case Some(tx) => getKnownTransactionFromEntityAction(tx).map(Some.apply)
    }

  def getTransactionsByAddress(address: Address): DBActionR[Seq[Transaction]] = {
    for {
      txHashes <- outputsTable
        .filter(_.address === address)
        .sortBy(_.timestamp.asc)
        .map(out => (out.txHash, out.timestamp))
        .distinct
        .result
      txs <- DBIOAction.sequence(txHashes.map {
        case (txHash, timestamp) => getKnownTransactionAction(txHash, timestamp)
      })
    } yield txs
  }

  private def getKnownTransactionFromEntityAction(tx: TransactionEntity): DBActionR[Transaction] =
    getKnownTransactionAction(tx.hash, tx.timestamp)

  private def getKnownTransactionAction(txHash: Transaction.Hash,
                                        timestamp: TimeStamp): DBActionR[Transaction] =
    for {
      ins        <- inputsTable.filter(_.txHash === txHash).result
      outs       <- outputsTable.filter(_.txHash === txHash).result
      insOutsRef <- DBIOAction.sequence(ins.map(getOutputFromInput))
    } yield
      Transaction(txHash,
                  timestamp,
                  AVector.from(insOutsRef.map { case (in, out) => in.toApi(out) }),
                  AVector.from(outs.map(_.toApi)))

  private def getOutputFromInput(
      input: InputEntity): DBActionR[(InputEntity, Option[OutputEntity])] =
    outputsTable
      .filter(_.outputRefKey === input.key)
      .result
      .headOption
      .map(output => (input, output))
}
