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
package org.alephium.explorer.util

import java.time._
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicInteger

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.jdk.CollectionConverters.CollectionHasAsScala

import org.scalacheck.Gen
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

import org.alephium.explorer.AlephiumSpec._
import org.alephium.explorer.util.TestUtils._

class SchedulerSpec
    extends AnyWordSpec
    with ScalaCheckDrivenPropertyChecks
    with Matchers
    with Eventually
    with ScalaFutures {

  implicit val executionContext: ExecutionContext = ExecutionContext.global

  val zoneIds: Iterable[ZoneId] =
    ZoneId.getAvailableZoneIds.asScala.map(ZoneId.of)

  "scheduleTime" when {
    "time is now" should {
      "return now/current/immediate duration" in {
        forAll(Gen.oneOf(zoneIds)) { zoneId =>
          //Time is now! Expect hours and minutes to be zero. Add 2 seconds to account for execution time.
          val timeLeft =
            Scheduler.scheduleTime(ZonedDateTime.now(zoneId).plusSeconds(2), "test-scheduler")

          timeLeft.toHours is 0
          timeLeft.toMinutes is 0
        }
      }
    }

    "time is in the future" should {
      "return today's duration" in {
        forAll(Gen.oneOf(zoneIds)) { zoneId =>
          //Hours
          Scheduler
            .scheduleTime(ZonedDateTime.now(zoneId).plusHours(2).plusMinutes(1), "test-scheduler")
            .toHours is 2
          //Minutes
          Scheduler
            .scheduleTime(ZonedDateTime.now(zoneId).plusMinutes(10).plusSeconds(2),
                          "test-scheduler")
            .toMinutes is 10
          //Days
          Scheduler
            .scheduleTime(ZonedDateTime.now(zoneId).plusDays(10).plusSeconds(2), "test-scheduler")
            .toDays is 10
        }
      }
    }

    "time is in the past" should {
      "return tomorrows duration" in {
        forAll(Gen.oneOf(zoneIds)) { zoneId =>
          //Time is 1 hour in the past so schedule happens 23 hours later
          Scheduler
            .scheduleTime(ZonedDateTime.now(zoneId).minusHours(1).plusSeconds(2), "test-scheduler")
            .toHours is 23
          //Time is 1 minute in the past so schedule happens after 23 hours (next day)
          Scheduler
            .scheduleTime(ZonedDateTime.now(zoneId).minusMinutes(1), "test-scheduler")
            .toHours is 23
          //Time is few seconds in the past so schedule happens after 23 hours (next day)
          Scheduler
            .scheduleTime(ZonedDateTime.now(zoneId).minusSeconds(3), "test-scheduler")
            .toHours is 23
          //1 year behind will still return 23 hours schedule time.
          Scheduler
            .scheduleTime(ZonedDateTime.now(zoneId).minusYears(1), "test-scheduler")
            .toHours is 23
        }
      }
    }
  }

  "scheduleLoop" should {
    "schedule tasks at regular fixed interval" in {
      using(Scheduler("test")) { scheduler =>
        //collects all scheduled task times
        val scheduleTimes = new ConcurrentLinkedDeque[Long]

        //schedule tasks every 1 second, first one being immediate
        scheduler.scheduleLoop("test", 0.seconds, 1.seconds) {
          Future(scheduleTimes.add(System.nanoTime()))
        }

        eventually(Timeout(10.seconds)) {
          //wait until at least 6 schedules have occurs
          scheduleTimes.size() should be > 6
        }

        val firstTime = scheduleTimes.pollFirst()

        //check that all scheduled tasks have 1 second difference.
        scheduleTimes.asScala.foldLeft(firstTime) {
          case (previous, next) =>
            //difference between previous scheduled task and next should not be
            //greater than 2 seconds and no less than 1 second.
            (next - previous).nanos.toMillis should (be > 995L and be <= 1900L)
            next
        }
      }
    }
  }

  "scheduleDailyAt" should {
    "schedule a task" when {
      "input is ZonedDateTime" in {
        forAll(Gen.oneOf(zoneIds)) { zoneId =>
          using(Scheduler("test")) { scheduler =>
            //starting time for the test
            val startTime               = System.currentTimeMillis() //test started
            @volatile var executionTime = 0L //scheduler executed

            scheduler.scheduleDailyAt("test", ZonedDateTime.now(zoneId).plusSeconds(3)) {
              Future {
                executionTime = System.currentTimeMillis()
              }
            }

            //scheduledTime - testStartTime is 3 seconds
            eventually(Timeout(5.seconds)) {
              //expect the task to be executed between [2.5 .. 3.9] seconds.
              (executionTime - startTime) should (be >= 2500L and be <= 3900L)
            }
          }
        }
      }

      "input is OffsetTime" in {
        forAll(Gen.oneOf(zoneIds)) { zoneId =>
          using(Scheduler("test")) { scheduler =>
            val now        = LocalDateTime.now(zoneId)
            val zoneOffSet = zoneId.getRules.getOffset(now)

            val startTime               = System.currentTimeMillis() //test started
            @volatile var executionTime = 0L //scheduler executed

            scheduler.scheduleDailyAt("test", OffsetTime.now(zoneOffSet).plusSeconds(4)) {
              Future {
                executionTime = System.currentTimeMillis()
              }
            }

            eventually(Timeout(10.seconds)) {
              //expect the task to be executed between [2.9 .. 4.9] seconds.
              (executionTime - startTime) should (be >= 2900L and be <= 4900L)
            }
          }
        }
      }
    }
  }

  "scheduleLoopFlatMap" should {
    "invoke init only once" in {
      using(Scheduler("test")) { scheduler =>
        val initInvocations  = new AtomicInteger(0) //# of times init was invoked
        val blockInvocations = new AtomicInteger(0) //# of times block was invoked

        //schedule tasks every 1 second, first one being immediate
        scheduler.scheduleLoopFlatMap("test", 0.seconds, 1.seconds) {
          //init
          Future(initInvocations.incrementAndGet())
        } { initInvoked: Int =>
          Future {
            //does not change even on multiple invocations.
            initInvoked is 1
            blockInvocations.incrementAndGet()
          }
        }

        eventually(Timeout(10.seconds)) {
          //wait until block is invoked at least 6 times
          blockInvocations.get() should be > 6
        }

        //init is only invoked once.
        initInvocations.get() is 1
      }
    }
  }
}
