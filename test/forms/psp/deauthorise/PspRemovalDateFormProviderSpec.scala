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

package forms.psp.deauthorise

import forms.mappings.Constraints
import org.scalatest.Matchers
import play.api.data.FormError
import views.behaviours.StringFieldBehaviours

import java.time.LocalDate

class PspRemovalDateFormProviderSpec extends StringFieldBehaviours with Constraints with Matchers {

  // scalastyle:off magic.number
  private val relationshipStartDate =  LocalDate.of(2020, 4, 1)
  def form() = new PspRemovalDateFormProvider()(relationshipStartDate, "messages__pspRemoval_date_error__before_relationshipStartDate")
  // scalastyle:on magic.number

  ".date" must {

    val fieldName = "pspRemovalDate"

    behave like mandatoryDateField(
      form,
      fieldName,
      "pspRemoval"
    )

    behave like dateFieldThatBindsValidData(
      form(),
      fieldName,
      historicDate()
    )

    val futureDate = LocalDate.now().plusDays(1)
    "not accept a future date" in {
      form().bind(
        Map(
          "pspRemovalDate.day" -> futureDate.getDayOfMonth.toString,
          "pspRemovalDate.month" -> futureDate.getMonthValue.toString,
          "pspRemovalDate.year" -> futureDate.getYear.toString
        )
      ).errors shouldBe Seq(FormError(fieldName, "messages__pspRemoval_date_error__future_date"))
    }

    "not accept a date before relationship start date for psa" in {
      form().bind(
        Map(
          "pspRemovalDate.day" -> relationshipStartDate.minusDays(1).getDayOfMonth.toString,
          "pspRemovalDate.month" -> relationshipStartDate.minusDays(1).getMonthValue.toString,
          "pspRemovalDate.year" -> relationshipStartDate.minusDays(1).getYear.toString
        )
      ).errors shouldBe Seq(FormError(fieldName, "messages__pspRemoval_date_error__before_relationshipStartDate"))
    }
  }

}
