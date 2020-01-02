/*
 * Copyright 2020 HM Revenue & Customs
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


class RemovalDateFormProvider @Inject() extends DateMapping with Constraints {

  val futureDate = "messages__removal_date_error__future_date"
  val beforeAssociation = "messages__removal_date_error__before_association"
  val beforeEarliest = "messages__removal_date_error__before_earliest_date"
  val dateErrors = DateErrors(
    "messages__removal_date_error__all_blank",
    "messages__removal_date_error__day_blank",
    "messages__removal_date_error__month_blank",
    "messages__removal_date_error__year_blank",
    "messages__removal_date_error__day_month_blank",
    "messages__removal_date_error__month_year_blank",
    "messages__removal_date_error__common"
  )

  def apply(associationDate: LocalDate, earliestDate: LocalDate): Form[LocalDate] =
    Form(
      "removalDate" -> dateMapping(dateErrors)
        .verifying(
          firstError(
            nonFutureDate(futureDate),
            afterGivenDate(beforeEarliest, earliestDate),
            afterGivenDate(beforeAssociation, associationDate)
          )
        )
    )
}
