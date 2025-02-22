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
import scala.concurrent.{ExecutionContext, Future}

import org.scalacheck.Gen
import slick.basic.DatabaseConfig
import slick.jdbc.PostgresProfile

import org.oxygenium.explorer._
import org.oxygenium.explorer.ConfigDefaults._
import org.oxygenium.explorer.GenApiModel._
import org.oxygenium.explorer.HttpFixture._
import org.oxygenium.explorer.api.model._
import org.oxygenium.explorer.persistence.DatabaseFixtureForAll
import org.oxygenium.explorer.service._
import org.oxygenium.protocol.model.TokenId

@SuppressWarnings(Array("org.wartremover.warts.Var"))
class TokenServerSpec()
    extends OxygeniumActorSpecLike
    with HttpServerFixture
    with DatabaseFixtureForAll {

  val tokenService = new EmptyTokenService {}
  val holdertokens = ArraySeq.from(Gen.listOf(holderInfoGen).sample.get)

  val holderService = new EmptyHolderService {
    override def getOxmHolders(pagination: Pagination)(implicit
        ec: ExecutionContext,
        dc: DatabaseConfig[PostgresProfile]
    ): Future[ArraySeq[HolderInfo]] = Future.successful(holdertokens)

    override def getTokenHolders(token: TokenId, pagination: Pagination)(implicit
        ec: ExecutionContext,
        dc: DatabaseConfig[PostgresProfile]
    ): Future[ArraySeq[HolderInfo]] = Future.successful(holdertokens)
  }

  val tokenServer =
    new TokenServer(tokenService, holderService)

  val routes = tokenServer.routes

  "return alph holders" in {
    Get(s"/tokens/holders/alph") check { response =>
      response.as[ArraySeq[HolderInfo]] is holdertokens
    }
  }

  "return token holders" in {
    Get(s"/tokens/holders/token/${tokenIdGen.sample.get.toHexString}") check { response =>
      response.as[ArraySeq[HolderInfo]] is holdertokens
    }
  }
}
