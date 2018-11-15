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
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.data.Forms._
import play.api.data.{Form, FormError}

class DateMappingSpec extends WordSpec with DateMapping with MustMatchers with OptionValues {

  "date" must {
    case class TestClass(date: LocalDate)

    val testForm = Form(
      mapping(
        "date" -> dateMapping("removal_date")
      )(TestClass.apply)(TestClass.unapply)
    )

    // scalastyle:off magic.number
    val testDate = new LocalDate(1862, 6, 9)
    // scalastyle:on magic.number

    "bind valid data" in {
      val result = testForm.bind(
        Map(
          "date.day" -> testDate.getDayOfMonth.toString,
          "date.month" -> testDate.getMonthOfYear.toString,
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
      result.errors must contain allOf(
        FormError("date.day", "messages__removal_date_error__day_blank"),
        FormError("date.month", "messages__removal_date_error__month_blank"),
        FormError("date.year", "messages__removal_date_error__year_blank")
      )
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
      result.errors must contain allOf(
        FormError("date.day", "messages__removal_date_error__nonNumeric_day"),
        FormError("date.month", "messages__removal_date_error__nonNumeric_month"),
        FormError("date.year", "messages__removal_date_error__nonNumeric_year")
      )
    }

    "not bind invalid day" in {
      val result = testForm.bind(
        Map(
          "date.day" -> "0",
          "date.month" -> testDate.getMonthOfYear.toString,
          "date.year" -> testDate.getYear.toString
        )
      )

      result.errors.size mustBe 1
      result.errors must contain(FormError("date.day", "messages__date_error__invalid_day_31"))
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
      result.errors must contain(FormError("date.month", "messages__date_error__invalid_month"))
    }

    "not bind invalid year" in {
      val result = testForm.bind(
        Map(
          "date.day" -> testDate.getDayOfMonth.toString,
          "date.month" -> testDate.getMonthOfYear.toString,
          "date.year" -> "123"
        )
      )

      result.errors.size mustBe 1
      result.errors must contain(FormError("date.year", "messages__date_error__invalid_year"))
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
