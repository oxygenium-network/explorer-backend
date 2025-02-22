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

package org.oxygenium.explorer.web

import scala.collection.immutable.ArraySeq

import org.oxygenium.api.ApiError.{BadRequest, NotFound}
import org.oxygenium.api.model.ValAddress
import org.oxygenium.explorer._
import org.oxygenium.explorer.ConfigDefaults._
import org.oxygenium.explorer.GenCoreProtocol._
import org.oxygenium.explorer.GenCoreUtil.timestampGen
import org.oxygenium.explorer.GenDBModel._
import org.oxygenium.explorer.HttpFixture._
import org.oxygenium.explorer.api.model._
import org.oxygenium.explorer.persistence.{DatabaseFixtureForAll, DBRunner}
import org.oxygenium.explorer.persistence.queries.BlockQueries
import org.oxygenium.explorer.persistence.queries.ContractQueries

@SuppressWarnings(
  Array(
    "org.wartremover.warts.PlatformDefault",
    "org.wartremover.warts.Var",
    "org.wartremover.warts.AsInstanceOf"
  )
)
class ContractServerSpec()
    extends OxygeniumActorSpecLike
    with DatabaseFixtureForAll
    with DBRunner
    with HttpServerFixture {

  val server = new ContractServer()

  val routes = server.routes

  "get contract liveness" should {
    "return the liveness when only 1 entry is in DB" in {
      val (groupIndex, events) = createEventsGen().sample.get
      run(ContractQueries.insertContractCreation(events, groupIndex)).futureValue

      events.foreach { event =>
        val address = event.fields.head.asInstanceOf[ValAddress].value
        Get(s"/contracts/${address}/current-liveness") check { response =>
          val contractInfo = response.as[ContractLiveness]
          contractInfo.creation.blockHash is event.blockHash
          contractInfo.creation.txHash is event.txHash
        }
      }
    }

    "return the latest liveness when multiple exists for one contract" in {
      val (groupIndex, baseEvents) = createEventsGen().sample.get
      val events = baseEvents ++ baseEvents.map(
        _.copy(blockHash = blockHashGen.sample.get, timestamp = timestampGen.sample.get)
      )
      val blockHeaders = events.map(event =>
        blockHeaderGen.sample.get.copy(hash = event.blockHash, mainChain = true)
      )

      run(ContractQueries.insertContractCreation(events, groupIndex)).futureValue
      run(BlockQueries.insertBlockHeaders(blockHeaders)).futureValue

      events.foreach { event =>
        val address = event.fields.head.asInstanceOf[ValAddress].value
        Get(s"/contracts/${address}/current-liveness") check { response =>
          val infos = run(ContractQueries.getContractEntity(address)).futureValue

          infos.size is 2
          infos.maxBy(_.creationTimestamp).toApi is response.as[ContractLiveness]
        }
      }
    }

    "return not found if liveness not in main chain" in {
      val (groupIndex, baseEvents) = createEventsGen().sample.get
      val events = baseEvents ++ baseEvents.map(
        _.copy(blockHash = blockHashGen.sample.get)
      )
      val blockHeaders = events.map(event =>
        blockHeaderGen.sample.get.copy(hash = event.blockHash, mainChain = false)
      )

      run(ContractQueries.insertContractCreation(events, groupIndex)).futureValue
      run(BlockQueries.insertBlockHeaders(blockHeaders)).futureValue

      events.foreach { event =>
        val address = event.fields.head.asInstanceOf[ValAddress].value
        Get(s"/contracts/${address}/current-liveness") check { response =>
          response.as[NotFound] is NotFound(s"Contract not found in main chain: $address")
        }
      }
    }

    "return not found when contract doesn't exist" in {
      forAll(addressContractProtocolGen) { address =>
        Get(s"/contracts/$address/current-liveness") check { response =>
          response.as[NotFound] is NotFound(s"Contract not found: $address")
        }
      }
    }
  }

  "get parent contract" in {
    forAll(addressContractProtocolGen) { address =>
      Get(s"/contracts/$address/parent") check { response =>
        response.as[ContractParent] is ContractParent(None)
      }
    }

    forAll(addressAssetProtocolGen()) { address =>
      Get(s"/contracts/$address/parent") check { response =>
        response.as[BadRequest] is BadRequest(
          s"Invalid value for: path parameter contract_address (Expect contract address, but was asset address: $address: $address)"
        )
      }
    }
  }

  "get sub-contracts" in {
    forAll(addressContractProtocolGen) { address =>
      Get(s"/contracts/$address/sub-contracts") check { response =>
        response.as[SubContracts] is SubContracts(ArraySeq.empty)
      }
    }

    forAll(addressAssetProtocolGen()) { address =>
      Get(s"/contracts/$address/sub-contracts") check { response =>
        response.as[BadRequest] is BadRequest(
          s"Invalid value for: path parameter contract_address (Expect contract address, but was asset address: $address: $address)"
        )
      }
    }
  }
}
