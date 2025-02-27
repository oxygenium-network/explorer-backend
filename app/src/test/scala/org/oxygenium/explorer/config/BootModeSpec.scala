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

package org.oxygenium.explorer.config

import scala.util.{Failure, Success}

import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

import org.oxygenium.explorer.OxygeniumSpec
import org.oxygenium.explorer.error.ExplorerError.InvalidBootMode

class BootModeSpec extends OxygeniumSpec with ScalaCheckDrivenPropertyChecks {

  "validate" should {
    "fail" when {
      "input mode is invalid" in {
        forAll { (mode: String) =>
          BootMode.validate(mode) is Failure(InvalidBootMode(mode))
        }
      }
    }

    "succeed" when {
      "input mode is valid" in {
        BootMode.all foreach { mode =>
          BootMode.validate(mode.productPrefix) is Success(mode)
        }
      }
    }
  }
}
