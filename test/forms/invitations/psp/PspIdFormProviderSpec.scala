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

class PspIdFormProviderSpec extends StringFieldBehaviours with Constraints{

  val formProvider = new PspIdFormProvider()
  val form = formProvider()

  ".pspId" must {

    val fieldName = "pspId"
    val requiredKey = "messages__error__pspId__required"
    val nonNumericKey = "messages__error__pspId__nonNumeric"
    val lengthKey = "messages__error__pspId__length"
    val invalidKey = "messages__error__pspId__invalid"
    val maxLength = PspIdFormProvider.pspIdLength

    "bind valid data" in {
        val result = form.bind(Map(fieldName -> "01234567")).apply(fieldName)
        result.errors shouldBe empty
    }

    s"not bind string longer than 8 characters" in {
        val result = form.bind(Map(fieldName -> "0123456789")).apply(fieldName)
        result.errors shouldEqual Seq(FormError(fieldName, lengthKey, Seq(maxLength)))
    }

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )

    behave like fieldWithRegex(
      form,
      fieldName,
      "B1234567",
      FormError(fieldName, nonNumericKey, Seq("""^[0-9]*$"""))
    )

    "not bind string not starting with 0, 1, or 2" in {
      val result = form.bind(Map(fieldName -> "31234567")).apply(fieldName)
      result.errors shouldEqual Seq(FormError(fieldName, invalidKey, Seq(Constraints.pspIdRegx)))
    }

    "remove spaces" in {
      val result = form.bind(Map(fieldName -> " 021 000 51 "))
      result.errors shouldBe empty
      result.value shouldBe Some("02100051")
    }
  }
}
