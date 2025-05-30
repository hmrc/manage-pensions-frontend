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

package forms.psp

import forms.behaviours.FieldBehaviours
import forms.mappings.Constraints
import play.api.data.FormError

class ListSchemesFormProviderSpec extends FieldBehaviours {

  val fieldName = "searchText"
  val requiredKey = "messages__listSchemesPsp__search_required"
  val invalidErrorKey = "messages__listSchemesPsp__search_invalid"
  val lengthErrorKey = "messages__listSchemesPsp__search_length"
  val validPstr = "24000001IN"
  val maxLength = 35

  val form = new ListSchemesFormProvider()()

  "ListSchemesFormProvider" must {

    fieldThatBindsValidData(
      form,
      fieldName,
      validPstr
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )

    behave like fieldWithRegex(
        form,
        fieldName,
    "1£!234",
        FormError(fieldName, invalidErrorKey, Seq(Constraints.searchRegx))
    )

    behave like fieldWithMaxLength(
      form,
      fieldName,
      maxLength,
      FormError(fieldName, lengthErrorKey, Seq(maxLength))
    )
  }
}
