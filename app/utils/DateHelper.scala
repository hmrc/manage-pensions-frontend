/*
 * Copyright 2021 HM Revenue & Customs
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicReference

object DateHelper {

  private val mockDate: AtomicReference[Option[LocalDate]] = new AtomicReference(None)

  def currentDate: LocalDate = mockDate.get().getOrElse(LocalDate.now())
  def setDate(date: Option[LocalDate]): Unit = mockDate.set(date)
  def overriddenDate: Option[LocalDate] = mockDate.get()

  val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")
  val startDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM")
  val endDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")
  def formatDate(date: LocalDate): String = {
    date.format(formatter)
  }

  def dateTimeFromNowToMidnightAfterDays(daysAhead: Int): LocalDateTime =
    currentDate.plusDays(daysAhead + 1).atStartOfDay()

  def displayExpiryDate(date: LocalDate): String = {
    formatDate(date.minusDays(1))
  }


}
