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

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime}

trait DateHelper {
  def currentDate: LocalDate = LocalDate.now()

  val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")
  def formatDate(date: LocalDate): String = {
    date.format(formatter)
  }

  def dateTimeFromNowToMidnightAfterDays(daysAhead: Int): LocalDateTime =
    currentDate.plusDays(daysAhead + 1).atStartOfDay()

  def displayExpiryDate(date: LocalDate): String = {
    formatDate(date.minusDays(1))
  }
}

object DateHelper extends DateHelper
