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
import play.api.data.Forms.{of, _}
import play.api.data.{FieldMapping, Mapping}
import utils.Enumerable

import scala.util.Try

trait Mappings extends Formatters with Constraints {

  protected def text(errorKey: String = "error.required"): FieldMapping[String] =
    of(stringFormatter(errorKey))

  protected def optionalText(): FieldMapping[Option[String]] =
    of(optionalStringFormatter)

  protected def int(requiredKey: String = "error.required",
                    wholeNumberKey: String = "error.wholeNumber",
                    nonNumericKey: String = "error.nonNumeric"): FieldMapping[Int] =
    of(intFormatter(requiredKey, wholeNumberKey, nonNumericKey))

  protected def boolean(requiredKey: String = "error.required",
                        invalidKey: String = "error.boolean"): FieldMapping[Boolean] =
    of(booleanFormatter(requiredKey, invalidKey))

  protected def enumerable[A](requiredKey: String = "error.required",
                              invalidKey: String = "error.invalid")(implicit ev: Enumerable[A]): FieldMapping[A] =
    of(enumerableFormatter[A](requiredKey, invalidKey))

  protected def dateMapping(invalidKey: String): Mapping[LocalDate] = {

    def toLocalDate(input: (Int, Int, Int)): LocalDate = {
      new LocalDate(input._3, input._2, input._1)
    }

    def fromLocalDate(date: LocalDate): (Int, Int, Int) = {
      (date.getDayOfMonth, date.getMonthOfYear, date.getYear)
    }

    def validDate(input: (Int, Int, Int)): Boolean = {
      Try(toLocalDate(input)).isSuccess
    }

    tuple(
      "day" -> int(requiredKey = "error.date.day_blank", wholeNumberKey = "error.date.day_invalid", nonNumericKey = "error.date.day_invalid"),
      "month" -> int(requiredKey = "error.date.month_blank", wholeNumberKey = "error.date.month_invalid", nonNumericKey = "error.date.month_invalid"),
      "year" -> int(requiredKey = "error.date.year_blank", wholeNumberKey = "error.date.year_invalid", nonNumericKey = "error.date.year_invalid")
    ).verifying(invalidKey, inputs => validDate(inputs))
      .transform(toLocalDate, fromLocalDate)

  }
}
