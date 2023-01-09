/*
 * Copyright 2023 HM Revenue & Customs
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

package forms.mappings


import forms.mappings.DateMapping.notRealDate
import play.api.data.Forms.{of, tuple}
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.data.{FieldMapping, Mapping}

import java.time.{DateTimeException, LocalDate}
import scala.util.{Failure, Success, Try}

trait DateMapping extends Formatters with Constraints {

  def validDate(genericError: String): Constraint[(Int, Int, Int)] = Constraint {
    input =>
      Try(toLocalDate(input)) match {
        case Failure(ex) if ex.isInstanceOf[DateTimeException] =>
          Invalid(notRealDate)
        case Failure(_) =>
          Invalid(genericError)
        case Success(_) =>
          Valid
      }
  }

  def dateMapping(errors: DateErrors): Mapping[LocalDate] = tuple(
    "day" -> int(requiredKey = errors.dayBlank, wholeNumberKey = "error.date.day_invalid", nonNumericKey =
      "error.date.day_invalid"),
    "month" -> int(requiredKey = errors.monthBlank, wholeNumberKey = "error.date.month_invalid",
      nonNumericKey = "error.date.month_invalid"),
    "year" -> int(requiredKey = errors.yearBlank, wholeNumberKey = "error.date.year_invalid", nonNumericKey
    = "error.date.year_invalid")
  ).verifying(validDate(errors.genericError))
    .transform[LocalDate](toLocalDate, fromLocalDate)


  def toLocalDate(date: (Int, Int, Int)): LocalDate =
    LocalDate.of(date._3, date._2, date._1)

  def fromLocalDate(date: LocalDate): (Int, Int, Int) =
    (date.getDayOfMonth, date.getMonthValue, date.getYear)

  protected def int(requiredKey: String = "error.required",
                    wholeNumberKey: String = "error.wholeNumber",
                    nonNumericKey: String = "error.nonNumeric"): FieldMapping[Int] =
    of(intFormatter(requiredKey, wholeNumberKey, nonNumericKey))
}

object DateMapping {
  val dayRegex = """^(0?[1-9]|[12][0-9]|3[01])$"""
  val monthRegex = """^(0?[1-9]|1[012])$"""
  val yearRegex = """^[1-9]{1}[0-9]{3}$"""
  val yearRangeRegex = """^(19[0-9][0-9]|20[0-4][0-9]|2050)$"""

  val notRealDate = "messages__date_error__real_date"
  val invalidYear = "messages__date_error__invalid_year"

}

case class DateErrors(
                       allBlank: String,
                       dayBlank: String,
                       monthBlank: String,
                       yearBlank: String,
                       dayMonthBlank: String,
                       monthYearBlank: String,
                       genericError: String
                     )
