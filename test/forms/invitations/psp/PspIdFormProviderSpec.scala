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
import wolfendale.scalacheck.regexp.RegexpGen

class PspIdFormProviderSpec extends StringFieldBehaviours with Constraints{

  val formProvider = new PspIdFormProvider()
  val form = formProvider()

  ".pspId" must {

    val fieldName = "pspId"
    val requiredKey = "messages__error__pspId__required"
    val lengthKey = "messages__error__pspId__length"
    val invalidKey = "messages__error__pspId__invalid"
    val maxLength = PspIdFormProvider.pspIdLength

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      RegexpGen.from(Constraints.pspIdRegx)
    )

    behave like fieldWithMaxLength(
      form,
      fieldName,
      maxLength = maxLength,
      lengthError = FormError(fieldName, lengthKey, Seq(maxLength))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )

    behave like fieldWithRegex(
      form,
      fieldName,
      "B1234567",
      FormError(fieldName, invalidKey, Seq(Constraints.pspIdRegx))
    )

    "remove spaces" in {
      val result = form.bind(Map(fieldName -> " 621 000 51 "))
      result.errors shouldBe empty
      result.value shouldBe Some("62100051")
    }
  }
}
