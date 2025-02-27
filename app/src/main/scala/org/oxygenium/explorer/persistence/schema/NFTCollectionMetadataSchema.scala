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

package org.oxygenium.explorer.persistence.schema

import slick.jdbc.PostgresProfile.api._
import slick.lifted.ProvenShape

import org.oxygenium.explorer.api.model.NFTCollectionMetadata
import org.oxygenium.explorer.persistence.schema.CustomJdbcTypes._
import org.oxygenium.protocol.model.Address

object NFTCollectionMetadataSchema
    extends SchemaMainChain[NFTCollectionMetadata]("nft_collection_metadata") {

  class TokenInfos(tag: Tag) extends Table[NFTCollectionMetadata](tag, name) {
    def contract: Rep[Address.Contract] = column[Address.Contract]("contract", O.PrimaryKey)
    def collectionUri: Rep[String]      = column[String]("collection_uri")

    def * : ProvenShape[NFTCollectionMetadata] =
      (contract, collectionUri)
        .<>((NFTCollectionMetadata.apply _).tupled, NFTCollectionMetadata.unapply)
  }

  val table: TableQuery[TokenInfos] = TableQuery[TokenInfos]
}
