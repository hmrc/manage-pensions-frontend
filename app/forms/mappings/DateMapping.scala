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

package forms.mappings

import org.joda.time.LocalDate
import play.api.data.Forms.{text, tuple}
import play.api.data.Mapping
import play.api.data.validation.{Constraint, Invalid, Valid}

import scala.util.{Failure, Success, Try}

trait DateMapping extends Constraints{

  import DateMapping._

  def individualValidation(errors: DateErrors): Constraint[(String, String, String)] = Constraint {
        case (day, month, year) if day.isEmpty && month.isEmpty && year.isEmpty => Invalid(errors.allBlank)
        case (day, month, _) if day.isEmpty && month.isEmpty => Invalid(errors.dayMonthBlank)
        case (_, month, year) if month.isEmpty && year.isEmpty => Invalid(errors.monthYearBlank)
        case (day, _, _) if day.isEmpty => Invalid(errors.dayBlank)
        case (_, month, _) if month.isEmpty => Invalid(errors.monthBlank)
        case (_, _, year) if year.isEmpty => Invalid(errors.yearBlank)
        case (_, month, _) if !month.matches(monthRegex) => Invalid(notRealDate)
        case _ => Valid
  }

  def validDate(genericError: String): Constraint[(String, String, String)] = Constraint {
    input =>
      Try(toLocalDate(input)) match {
        case Failure(exception) if exception.getMessage.contains("range [1,") =>
          Invalid(notRealDate)
        case Failure(_) => Invalid(genericError)
        case Success(_) => Valid
      }
  }

  def dateMapping(errors: DateErrors): Mapping[LocalDate] = tuple(
    "day" -> text,
    "month" -> text,
    "year" -> text
  ).verifying(firstError(individualValidation(errors), validDate(errors.genericError)))
   .transform[LocalDate](toLocalDate, fromLocalDate)


  def toLocalDate(date: (String, String, String)): LocalDate =
    new LocalDate(date._3.toInt, date._2.toInt, date._1.toInt)

  def fromLocalDate(date: LocalDate): (String, String, String) =
    (date.dayOfMonth().get().toString, date.monthOfYear().get().toString, date.year().get().toString)

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
      genericError : String
     )