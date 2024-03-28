/*
 * Copyright 2024 HM Revenue & Customs
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

import java.time.{Instant, ZoneId}
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicReference

object DateHelper {

  private val mockDate: AtomicReference[Option[Instant]] = new AtomicReference(None)

  def currentDate: Instant = mockDate.get().getOrElse(Instant.now())
  def setDate(date: Option[Instant]): Unit = mockDate.set(date)
  def overriddenDate: Option[Instant] = mockDate.get()

  val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")
  val auditFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
  val startDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM")
  val endDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")
  def formatDate(date: Instant): String = {
    formatter.format(date.atZone(ZoneId.of("UTC")))
  }

  def dateTimeFromNowToMidnightAfterDays(daysAhead: Int): Instant =
    currentDate.plus(daysAhead + 1, ChronoUnit.DAYS)

  def displayExpiryDate(date: Instant): String = {
    formatDate(date.minus(1, ChronoUnit.DAYS))
  }


}
