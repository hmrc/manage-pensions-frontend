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

package forms.remove

import java.time.LocalDate

import forms.mappings._
import javax.inject.Inject
import play.api.data.Form


class PspRemovalDateFormProvider @Inject() extends DateMapping with Constraints {

  val beforeAssociation = "messages__pspRemoval_date_error__before_relationshipStartDate"
  val futureDateError = "messages__pspRemoval_date_error__future_date"
  val dateErrors: DateErrors = DateErrors(
    "messages__pspRemoval_date_error__all_blank",
    "messages__pspRemoval_date_error__day_blank",
    "messages__pspRemoval_date_error__month_blank",
    "messages__pspRemoval_date_error__year_blank",
    "messages__pspRemoval_date_error__day_month_blank",
    "messages__pspRemoval_date_error__month_year_blank",
    "messages__pspRemoval_date_error__common"
  )

  def apply(relationshipStartDate: LocalDate, earliestDateError: String): Form[LocalDate] =
    Form(
      "pspRemovalDate" -> dateMapping(dateErrors)
        .verifying(
          firstError(
            nonFutureDate(futureDateError),
            afterGivenDate(earliestDateError, relationshipStartDate)
          )
        )
    )
}
