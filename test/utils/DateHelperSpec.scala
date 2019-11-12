/*
 * Copyright 2019 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package utils

import java.time.LocalDateTime

import org.scalatest.{Matchers, WordSpec}

class DateHelperSpec extends WordSpec with Matchers {

  val dateHelper = new DateHelper {
    override val currentDate = LocalDateTime.parse("2018-01-04T00:00:01Z").toLocalDate
  }

  val daysAhead = 30

  "thirtyDaysFromNowInSeconds" should {
    "respond correctly for a date at 1 second after midnight" in {
      val result = dateHelper.dateTimeFromNowToMidnightAfterDays(daysAhead).toString
      result shouldBe "2018-02-04T00:00:00.000Z"
    }
    "respond correctly for a date around the middle of the day" in {
      val result = dateHelper.dateTimeFromNowToMidnightAfterDays(daysAhead).toString
      result shouldBe "2018-02-04T00:00:00.000Z"
    }
    "respond correctly for a date at 1 second to midnight" in {
      val result = dateHelper.dateTimeFromNowToMidnightAfterDays(daysAhead).toString
      result shouldBe "2018-02-04T00:00:00.000Z"
    }
  }

  "displayExpiryDate " should {

    "return one day less with the correct format" in {
      val result = dateHelper.displayExpiryDate(dateHelper.currentDate)
      result shouldBe "3 January 2018"
    }
  }
}

