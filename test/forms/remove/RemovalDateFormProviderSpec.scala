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

package forms.remove

import forms.mappings.Constraints
import org.joda.time.LocalDate
import org.scalatest.Matchers
import play.api.data.FormError
import views.behaviours.StringFieldBehaviours

class RemovalDateFormProviderSpec extends StringFieldBehaviours with Constraints with Matchers {

  // scalastyle:off magic.number
  private val openedDate =  new LocalDate(2018, 1, 1)
  private val beforeOpenDate = new LocalDate(2017, 1,1)
  val form = new RemovalDateFormProvider()(openedDate)
  // scalastyle:on magic.number

  ".date" must {

    val fieldName = "removalDate"

    behave like dateFieldThatBindsValidData(
      form,
      fieldName,
      historicDate()
    )

    val futureDate = LocalDate.now().plusDays(1)
    "not accept a future date" in {
      form.bind(
        Map(
          "removalDate.day" -> futureDate.getDayOfMonth.toString,
          "removalDate.month" -> futureDate.getMonthOfYear.toString,
          "removalDate.year" -> futureDate.getYear.toString
        )
      ).errors shouldBe Seq(FormError(fieldName, "messages__removal_date_error__future_date"))
    }

    "not accept a date before open date" in {
      form.bind(
        Map(
          "removalDate.day" -> beforeOpenDate.getDayOfMonth.toString,
          "removalDate.month" -> beforeOpenDate.getMonthOfYear.toString,
          "removalDate.year" -> beforeOpenDate.getYear.toString
        )
      ).errors shouldBe Seq(FormError(fieldName, "messages__removal_date_error__before_scheme_start"))
    }
  }

}
