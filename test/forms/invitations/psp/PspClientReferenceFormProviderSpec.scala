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

package forms.invitations.psp

import forms.mappings.Constraints
import play.api.data.FormError
import views.behaviours.StringFieldBehaviours

class PspClientReferenceFormProviderSpec extends StringFieldBehaviours with Constraints {
  val validData: Map[String, String] = Map(
    "value.yesNo" -> "true",
    "value.reference" -> "some value")
  val validMaxLength = 160
  val form = new PspClientReferenceFormProvider()()

  ".value.yesNo" must {
    val fieldName = "value.yesNo"
    val requiredKey = "messages__clientReference_yes_no_required"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      "true"
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
