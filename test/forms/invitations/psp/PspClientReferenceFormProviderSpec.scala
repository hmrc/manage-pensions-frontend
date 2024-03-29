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

package forms.invitations.psp

import forms.mappings.Constraints
import play.api.data.FormError
import views.behaviours.StringFieldBehaviours

class PspClientReferenceFormProviderSpec extends StringFieldBehaviours with Constraints {
  val validMaxLength = 11
  val form = new PspClientReferenceFormProvider()()

  ".value.reference" must {
    val fieldName = "reference"
    val requiredKey = "messages__clientReference_required"
    val invalidKey = "messages__clientReference_invalid"

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

    "not bind string invalidated by regex" in {
      val result = form.bind(Map("reference" -> "$&^"))
      result.errors shouldEqual Seq(FormError("reference", invalidKey, Seq(Constraints.clientRefRegx)))
    }

    "bind string with spaces, removing the spaces" in {
      val result = form.bind(Map("reference" -> "A B C"))
      result.errors shouldBe empty
      result.value shouldBe Some("ABC")
    }
  }
}
