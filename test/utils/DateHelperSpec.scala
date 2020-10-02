/*
 * Copyright 2020 HM Revenue & Customs
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

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import org.scalatest.Matchers
import org.scalatest.WordSpec

class DateHelperSpec extends WordSpec with Matchers {

  val currentDate: LocalDate = LocalDate.parse("2018-01-04T00:00:01", DateTimeFormatter.ISO_LOCAL_DATE_TIME)

  val daysAhead = 30

  "thirtyDaysFromNowInSeconds" should {
    "respond correctly for a date at 1 second after midnight" in {
      DateHelper.setDate(Some(currentDate))
      val result = DateHelper.dateTimeFromNowToMidnightAfterDays(daysAhead).toString
      result shouldBe "2018-02-04T00:00"
    }
    "respond correctly for a date around the middle of the day" in {
      DateHelper.setDate(Some(currentDate))
      val result = DateHelper.dateTimeFromNowToMidnightAfterDays(daysAhead).toString
      result shouldBe "2018-02-04T00:00"
    }
    "respond correctly for a date at 1 second to midnight" in {
      DateHelper.setDate(Some(currentDate))
      val result = DateHelper.dateTimeFromNowToMidnightAfterDays(daysAhead).toString
      result shouldBe "2018-02-04T00:00"
    }
  }

  "displayExpiryDate " should {

    "return one day less with the correct format" in {
      DateHelper.setDate(Some(currentDate))
      val result = DateHelper.displayExpiryDate(DateHelper.currentDate)
      result shouldBe "3 January 2018"
    }
  }
}

