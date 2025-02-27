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

package org.oxygenium.explorer.api.model

import scala.concurrent.Future

import org.scalacheck.Gen

import org.oxygenium.api.ApiError
import org.oxygenium.api.model.TimeInterval
import org.oxygenium.explorer.OxygeniumFutureSpec
import org.oxygenium.explorer.GenApiModel.intervalTypeGen
import org.oxygenium.util.{Duration, TimeStamp}

class IntervalTypeSpec() extends OxygeniumFutureSpec {
  "IntervalType" should {
    "validate TimeInterval" in {
      val now = TimeStamp.now()
      forAll(intervalTypeGen, Gen.choose(1L, 10L)) { case (intervalType, amount) =>
        val duration     = (intervalType.duration * amount).get
        val to           = now + duration
        val timeInterval = TimeInterval(now, to)

        IntervalType
          .validateTimeInterval(timeInterval, intervalType, duration, duration, duration)(
            Future.successful(())
          )
          .futureValue is Right(())

        val shorterDuration = (duration - (Duration.ofMillisUnsafe(1))).get

        IntervalType
          .validateTimeInterval(
            timeInterval,
            intervalType,
            shorterDuration,
            shorterDuration,
            shorterDuration
          )(
            Future.successful(())
          )
          .futureValue is Left(
          ApiError.BadRequest(s"Time span cannot be greater than $shorterDuration")
        )
      }
    }
  }
}
