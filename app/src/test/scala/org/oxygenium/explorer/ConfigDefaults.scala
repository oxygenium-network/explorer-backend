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

import org.oxygenium.explorer.config.ExplorerConfig._
import org.oxygenium.protocol.config.GroupConfig
import org.oxygenium.util.Duration

object ConfigDefaults {
  implicit val groupSetting: GroupSetting = Generators.groupSettingGen.sample.get
  implicit val groupConfig: GroupConfig   = groupSetting.groupConfig

  val maxTimeIntervals: MaxTimeIntervals =
    MaxTimeIntervals(
      amountHistory = MaxTimeInterval(
        hourly = Duration.ofDaysUnsafe(7),
        daily = Duration.ofDaysUnsafe(366),
        weekly = Duration.ofDaysUnsafe(366)
      ),
      charts = MaxTimeInterval(
        hourly = Duration.ofDaysUnsafe(30),
        daily = Duration.ofDaysUnsafe(366),
        weekly = Duration.ofDaysUnsafe(366)
      ),
      exportTxs = Duration.ofDaysUnsafe(366)
    )
}
