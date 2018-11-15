/*
 * Copyright 2018 HM Revenue & Customs
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
import play.api.data.Forms.{of, tuple}
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.data.{FieldMapping, Mapping}

import scala.util.Try

trait DateMapping extends Mappings with Transforms {

  import DateMapping._

  protected def day(key: String): FieldMapping[Int] =

    of(intFormatterWithRegex(s"messages__${key}_error__day_blank",
      "messages__date_error__invalid_day_31", s"messages__${key}_error__nonNumeric_day", dayRegex))

  protected def month(key: String): FieldMapping[Int] =
    of(intFormatterWithRegex(s"messages__${key}_error__month_blank",
      "messages__date_error__invalid_month", s"messages__${key}_error__nonNumeric_month", monthRegex))

  protected def year(key: String): FieldMapping[Int] =
    of(intFormatterWithRegex(s"messages__${key}_error__year_blank",
      "messages__date_error__invalid_year", s"messages__${key}_error__nonNumeric_year", yearRegex))

  //scalastyle:off cyclomatic.complexity
  protected def dateMapping(key: String): Mapping[LocalDate] = {

    def toLocalDate(input: (Int, Int, Int)): LocalDate = {
      new LocalDate(input._3, input._2, input._1)
    }

    def fromLocalDate(date: LocalDate): (Int, Int, Int) = {
      (date.getDayOfMonth, date.getMonthOfYear, date.getYear)
    }

    def validDate: Constraint[(Int, Int, Int)] = Constraint {
      input =>
        Try(toLocalDate(input)) match {
          case scala.util.Failure(exception) if exception.getMessage.contains("range [1,28]") =>
            Invalid(invalidDay28)
          case scala.util.Failure(exception) if exception.getMessage.contains("range [1,29]") =>
            Invalid(invalidDay29)
          case scala.util.Failure(exception) if exception.getMessage.contains("range [1,30]") =>
            Invalid(invalidDay30)
          case scala.util.Failure(exception) if exception.getMessage.contains("range [1,31]") =>
            Invalid(invalidDay31)
          case scala.util.Success(_) => Valid
          case _ => Invalid(s"messages__${key}_error__common")
        }
    }

    tuple("day" -> day(key), "month" -> month(key), "year" -> year(key)
    ).verifying(
      firstError(
        validDate
      )
    )
      .transform(toLocalDate, fromLocalDate)

  }
}

object DateMapping {


  val dayRegex = """^(0?[1-9]|[12][0-9]|3[01])$"""
  val monthRegex = """^(0?[1-9]|1[012])$"""
  val yearRegex = """^[1-9]{1}[0-9]{3}$"""
  val yearRangeRegex = """^(19[0-9][0-9]|20[0-4][0-9]|2050)$"""

  val invalidDay28 = "messages__date_error__invalid_day_28"
  val invalidDay29 = "messages__date_error__invalid_day_29"
  val invalidDay30 = "messages__date_error__invalid_day_30"
  val invalidDay31 = "messages__date_error__invalid_day_31"
  val invalidMonth = "messages__date_error__invalid_month"
  val invalidYear = "messages__date_error__invalid_year"
}

