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

package org.alephium.explorer.api

import sttp.tapir._
import sttp.tapir.CodecFormat.TextPlain

import org.alephium.api.TapirCodecs
import org.alephium.api.model.TimeInterval
import org.alephium.explorer.api.Codecs._
import org.alephium.explorer.api.model.{IntervalType, Pagination}
import org.alephium.protocol.model.TokenId
import org.alephium.util.TimeStamp

trait QueryParams extends TapirCodecs {

  implicit val tokenIdTapirCodec: Codec[String, TokenId, TextPlain] =
    fromJson[TokenId]

  val pagination: EndpointInput[Pagination] =
    paginator(Pagination.maxLimit)

  def paginator(maxLimit: Int): EndpointInput[Pagination] =
    query[Option[Int]]("page")
      .description("Page number")
      .map({
        case Some(offset) => offset
        case None         => Pagination.defaultPage
      })(Some(_))
      .validate(Validator.min(1))
      .and(
        query[Option[Int]]("limit")
          .description("Number per page")
          .map({
            case Some(limit) => limit
            case None        => Pagination.defaultLimit
          })(Some(_))
          .validate(Validator.min(0))
          .validate(Validator.max(maxLimit)))
      .and(query[Option[Boolean]]("reverse")
        .description("Reverse pagination")
        .map({
          case Some(reverse) => reverse
          case None          => false
        })(Some(_)))
      .map({ case (offset, limit, reverse) => Pagination.unsafe(offset - 1, limit, reverse) })(p =>
        (p.offset, p.limit, p.reverse))

  val timeIntervalQuery: EndpointInput[TimeInterval] =
    query[TimeStamp]("fromTs")
      .and(query[TimeStamp]("toTs"))
      .map({ case (from, to) => TimeInterval(from, to) })(timeInterval =>
        (timeInterval.from, timeInterval.to))
      .validate(TimeInterval.validator)

  val intervalTypeQuery: EndpointInput[IntervalType] =
    query[IntervalType]("interval-type")

}
