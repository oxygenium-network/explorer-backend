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

package org.oxygenium.explorer.api

import scala.util.{Failure, Success, Try}

import sttp.tapir.{Codec, CodecFormat, DecodeResult}
import sttp.tapir.Codec.PlainCodec

import org.oxygenium.api.TapirCodecs
import org.oxygenium.explorer.api.model._
import org.oxygenium.json.Json._
import org.oxygenium.protocol.model.Address

object Codecs extends TapirCodecs {
  implicit val explorerAddressTapirCodec: PlainCodec[Address] =
    fromJson[Address]

  @SuppressWarnings(
    Array(
      "org.wartremover.warts.JavaSerializable",
      "org.wartremover.warts.Product",
      "org.wartremover.warts.Serializable"
    )
  ) // Wartremover is complaining, maybe beacause of tapir macros
  implicit val timeIntervalCodec: PlainCodec[IntervalType] =
    Codec.derivedEnumeration[String, IntervalType](
      IntervalType.validate,
      _.string
    )

  implicit val tokenStdInterfaceIdCodec: Codec[String, TokenStdInterfaceId, CodecFormat.TextPlain] =
    fromJson[TokenStdInterfaceId](
      StdInterfaceId.tokenReadWriter,
      StdInterfaceId.tokenWithHexStringSchema
    )

  def explorerFromJson[A: ReadWriter]: PlainCodec[A] =
    Codec.string.mapDecode[A] { raw =>
      Try(read[A](ujson.Str(raw))) match {
        case Success(a) => DecodeResult.Value(a)
        case Failure(error) =>
          DecodeResult.Error(raw, new IllegalArgumentException(error.getMessage))
      }
    } { a =>
      writeJs(a) match {
        case ujson.Str(str) => str
        case other          => write(other)
      }
    }
}
