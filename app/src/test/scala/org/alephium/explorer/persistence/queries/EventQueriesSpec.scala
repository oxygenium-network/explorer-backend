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

import scala.collection.immutable.ArraySeq

import org.scalacheck.Gen
import org.scalatest.concurrent.ScalaFutures
import slick.jdbc.PostgresProfile.api._

import org.oxygenium.explorer.OxygeniumFutureSpec
import org.oxygenium.explorer.ConfigDefaults._
import org.oxygenium.explorer.GenApiModel._
import org.oxygenium.explorer.GenCoreProtocol.transactionHashGen
import org.oxygenium.explorer.GenDBModel._
import org.oxygenium.explorer.api.model.Pagination
import org.oxygenium.explorer.persistence.{DatabaseFixtureForEach, DBRunner}
import org.oxygenium.explorer.persistence.model.EventEntity
import org.oxygenium.explorer.persistence.queries.EventQueries
import org.oxygenium.explorer.persistence.schema.EventSchema

class EventQueriesSpec
    extends OxygeniumFutureSpec
    with DatabaseFixtureForEach
    with DBRunner
    with ScalaFutures {

  val pagination = Pagination.unsafe(1, Pagination.defaultLimit)

  "Event Queries" should {
    "get event by tx hash" in {
      forAll(Gen.nonEmptyListOf(eventEntityGen())) { events =>
        insert(events)

        events.map { event =>
          val result = run(EventQueries.getEventsByTxIdQuery(event.txHash)).futureValue
          result.size is 1
          result.head.toApi is event.toApi
        }
      }
    }

    "get all events with same tx hash" in {
      forAll(Gen.nonEmptyListOf(eventEntityGen())) { events =>
        val txHash = transactionHashGen.sample.get
        val uniqueTxHashEvents = events.zipWithIndex.map { case (event, order) =>
          event.copy(txHash = txHash, eventOrder = order)
        }

        insert(uniqueTxHashEvents)

        val result = run(EventQueries.getEventsByTxIdQuery(txHash)).futureValue

        result.size is uniqueTxHashEvents.size
        result.zip(uniqueTxHashEvents.sortBy(_.eventOrder)).map { case (res, event) =>
          res.toApi is event.toApi
        }
      }
    }

    "get event by contract address" in {
      forAll(Gen.nonEmptyListOf(eventEntityGen())) { events =>
        insert(events)

        events.map { event =>
          val result =
            run(
              EventQueries.getEventsByContractAddressQuery(event.contractAddress, None, pagination)
            ).futureValue
          result.size is 1
          result.head.toApi is event.toApi
        }
      }
    }

    "get all events with same contractAddress" in {
      forAll(Gen.nonEmptyListOf(eventEntityGen())) { events =>
        val contractAddress = addressGen.sample.get
        val uniqueContractAddressEvents = events.map { event =>
          event.copy(contractAddress = contractAddress)
        }

        insert(uniqueContractAddressEvents)

        val fullPagination = Pagination.unsafe(1, uniqueContractAddressEvents.size)

        val result =
          run(
            EventQueries.getEventsByContractAddressQuery(contractAddress, None, fullPagination)
          ).futureValue

        result.size is uniqueContractAddressEvents.size
        result.zip(uniqueContractAddressEvents.sortBy(_.timestamp).reverse).map {
          case (res, event) =>
            res.toApi is event.toApi
        }

        val paginatedResult =
          run(
            EventQueries.getEventsByContractAddressQuery(contractAddress, None, pagination)
          ).futureValue

        if (uniqueContractAddressEvents.sizeIs > pagination.limit) {
          paginatedResult.size is pagination.limit
        } else {
          paginatedResult.size is uniqueContractAddressEvents.size
        }
      }
    }

    "filter event by event index" in {
      forAll(Gen.nonEmptyListOf(eventEntityGen())) { events =>
        val events2 = events.map(event =>
          event.copy(eventOrder = event.eventOrder + 1, eventIndex = event.eventIndex + 1)
        )

        insert(events ++ events2)

        events.map { event =>
          val withoutFilter =
            run(
              EventQueries.getEventsByContractAddressQuery(event.contractAddress, None, pagination)
            ).futureValue

          withoutFilter.size is 2

          val result =
            run(
              EventQueries.getEventsByContractAddressQuery(
                event.contractAddress,
                Some(event.eventIndex),
                pagination
              )
            ).futureValue

          result.size is 1
          result.head.toApi is event.toApi
        }
      }
    }

    "get event by contract address and input address" in {
      forAll(Gen.nonEmptyListOf(eventEntityGen())) { events =>
        insert(events)

        events.map { event =>
          event.inputAddress match {
            case Some(inputAddress) =>
              val result =
                run(
                  EventQueries.getEventsByContractAndInputAddressQuery(
                    event.contractAddress,
                    inputAddress,
                    None,
                    pagination
                  )
                ).futureValue

              result.size is 1
              result.head.toApi is event.toApi
            case None =>
              run(
                EventQueries.getEventsByContractAndInputAddressQuery(
                  event.contractAddress,
                  addressGen.sample.get,
                  None,
                  pagination
                )
              ).futureValue is ArraySeq.empty

          }
        }
      }
    }

    "get all events with same contractAddress and input address" in {
      forAll(Gen.nonEmptyListOf(eventEntityGen())) { events =>
        val contractAddress = addressGen.sample.get
        val inputAddress    = addressGen.sample.get
        val uniqueContractAddressEvents = events.map { event =>
          event.copy(contractAddress = contractAddress, inputAddress = Some(inputAddress))
        }

        insert(uniqueContractAddressEvents)

        val fullPagination = Pagination.unsafe(1, uniqueContractAddressEvents.size)

        val result =
          run(
            EventQueries.getEventsByContractAndInputAddressQuery(
              contractAddress,
              inputAddress,
              None,
              fullPagination
            )
          ).futureValue

        result.size is uniqueContractAddressEvents.size
        result.zip(uniqueContractAddressEvents.sortBy(_.timestamp).reverse).map {
          case (res, event) =>
            res.toApi is event.toApi
        }

        val paginatedResult =
          run(
            EventQueries.getEventsByContractAndInputAddressQuery(
              contractAddress,
              inputAddress,
              None,
              pagination
            )
          ).futureValue

        if (uniqueContractAddressEvents.sizeIs > pagination.limit) {
          paginatedResult.size is pagination.limit
        } else {
          paginatedResult.size is uniqueContractAddressEvents.size
        }
      }
    }
  }

  "get event by contract address, input address and filter event index" in {
    forAll(Gen.nonEmptyListOf(eventEntityGen())) { events =>
      val events2 = events.map(event =>
        event.copy(eventOrder = event.eventOrder + 1, eventIndex = event.eventIndex + 1)
      )

      insert(events ++ events2)

      events.map { event =>
        event.inputAddress match {
          case Some(inputAddress) =>
            val withoutFilter =
              run(
                EventQueries.getEventsByContractAndInputAddressQuery(
                  event.contractAddress,
                  inputAddress,
                  None,
                  pagination
                )
              ).futureValue

            withoutFilter.size is 2

            val result =
              run(
                EventQueries.getEventsByContractAndInputAddressQuery(
                  event.contractAddress,
                  inputAddress,
                  Some(event.eventIndex),
                  pagination
                )
              ).futureValue

            result.size is 1

          case None =>
            run(
              EventQueries.getEventsByContractAndInputAddressQuery(
                event.contractAddress,
                addressGen.sample.get,
                Some(event.eventIndex),
                pagination
              )
            ).futureValue is ArraySeq.empty
        }
      }
    }
  }
  def insert(events: ArraySeq[EventEntity]) = {
    run(EventSchema.table.delete).futureValue
    run(EventSchema.table ++= events).futureValue
  }
}
