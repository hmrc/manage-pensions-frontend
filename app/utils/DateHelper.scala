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

import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import org.joda.time.{DateTime, DateTimeZone, LocalDate}
import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads._

trait DateHelper {
  def currentDate: DateTime = DateTime.now(DateTimeZone.UTC)

  val formatter: DateTimeFormatter = DateTimeFormat.forPattern("d MMMM yyyy")
  def formatDate(date: LocalDate): String = {
    formatter.print(date)
  }

  def dateTimeFromNowToMidnightAfterDays(daysAhead:Int): DateTime =
    currentDate.toLocalDate.plusDays(daysAhead + 1).toDateTimeAtStartOfDay()

  def displayExpiryDate(date: LocalDate): String = {
    formatDate(date.minusDays(1))
  }
}

object DateHelper extends DateHelper
