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

package forms.mappings

import org.scalatest.OptionValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.data.Forms._
import play.api.data.{Form, FormError}

import java.time.LocalDate

class DateMappingSpec extends AnyWordSpec with DateMapping with Matchers with OptionValues {

  val dateErrors: DateErrors = DateErrors(
    "messages__removal_date_error__all_blank",
    "messages__removal_date_error__day_blank",
    "messages__removal_date_error__month_blank",
    "messages__removal_date_error__year_blank",
    "messages__removal_date_error__day_month_blank",
    "messages__removal_date_error__month_year_blank",
    "messages__removal_date_error__common"
  )

  "date" must {
    case class TestClass(date: LocalDate)

    val testForm = Form(
      mapping(
        "date" -> dateMapping(dateErrors)
      )(TestClass.apply)(o => Some(o.date))
    )

    // scalastyle:off magic.number
    val testDate = LocalDate.of(1862, 6, 9)
    // scalastyle:on magic.number

    "bind valid data" in {
      val result = testForm.bind(
        Map(
          "date.day" -> testDate.getDayOfMonth.toString,
          "date.month" -> testDate.getMonthValue.toString,
          "date.year" -> testDate.getYear.toString
        )
      )

      result.errors.size mustBe 0
      result.get.date mustBe testDate
    }

    "not bind blank data" in {
      val result = testForm.bind(
        Map(
          "date.day" -> "",
          "date.month" -> "",
          "date.year" -> ""
        )
      )

      result.errors.size mustBe 3
      result.errors mustBe List(FormError("date.day", dateErrors.dayBlank),
        FormError("date.month", dateErrors.monthBlank),
        FormError("date.year", dateErrors.yearBlank))
    }

    "not bind blank day" in {
      val result = testForm.bind(
        Map(
          "date.day" -> "",
          "date.month" -> "12",
          "date.year" -> "2018"
        )
      )

      result.errors.size mustBe 1
      result.errors mustBe List(FormError("date.day", dateErrors.dayBlank))
    }

    "not bind blank month" in {
      val result = testForm.bind(
        Map(
          "date.day" -> "2",
          "date.month" -> "",
          "date.year" -> "2009"
        )
      )

      result.errors.size mustBe 1
      result.errors mustBe List(FormError("date.month", dateErrors.monthBlank))
    }

    "not bind blank year" in {
      val result = testForm.bind(
        Map(
          "date.day" -> "12",
          "date.month" -> "12",
          "date.year" -> ""
        )
      )

      result.errors.size mustBe 1
      result.errors mustBe List(FormError("date.year", dateErrors.yearBlank))
    }

    "not bind blank day and month" in {
      val result = testForm.bind(
        Map(
          "date.day" -> "",
          "date.month" -> "",
          "date.year" -> "1999"
        )
      )

      result.errors.size mustBe 2
      result.errors mustBe List(FormError("date.day", dateErrors.dayBlank),
        FormError("date.month", dateErrors.monthBlank))
    }

    "not bind blank month and year" in {
      val result = testForm.bind(
        Map(
          "date.day" -> "3",
          "date.month" -> "",
          "date.year" -> ""
        )
      )

      result.errors.size mustBe 2
      result.errors mustBe List(FormError("date.month", dateErrors.monthBlank),
        FormError("date.year", dateErrors.yearBlank))
    }

    "not bind non-numeric input" in {
      val result = testForm.bind(
        Map(
          "date.day" -> "A",
          "date.month" -> "B",
          "date.year" -> "C"
        )
      )

      result.errors.size mustBe 3
      result.errors mustBe List(FormError("date.day", "error.date.day_invalid"),
        FormError("date.month", "error.date.month_invalid"),
        FormError("date.year", "error.date.year_invalid"))
    }

    "not bind invalid day" in {
      val result = testForm.bind(
        Map(
          "date.day" -> "0",
          "date.month" -> testDate.getMonthValue.toString,
          "date.year" -> testDate.getYear.toString
        )
      )

      result.errors.size mustBe 1
      result.errors mustBe List(FormError("date", "messages__date_error__real_date"))
    }

    "not bind invalid month" in {
      val result = testForm.bind(
        Map(
          "date.day" -> testDate.getDayOfMonth.toString,
          "date.month" -> "0",
          "date.year" -> testDate.getYear.toString
        )
      )

      result.errors.size mustBe 1
      result.errors mustBe List(FormError("date", "messages__date_error__real_date"))
    }

    "fill correctly from model" in {
      val testClass = TestClass(testDate)
      val result = testForm.fill(testClass)

      result("date.day").value.value mustBe "9"
      result("date.month").value.value mustBe "6"
      result("date.year").value.value mustBe "1862"
    }
  }

}
