/*
 * Copyright 2022 HM Revenue & Customs
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

class PspNameFormProviderSpec extends StringFieldBehaviours with Constraints{

  val formProvider = new PspNameFormProvider()
  val form = formProvider()

  ".pspName" must {

    val fieldName = "pspName"
    val requiredKey = "messages__error__pspName__required"
    val lengthKey = "messages__error__pspName__length"
    val invalidKey = "messages__error__pspName__invalid"
    val maxLength = PspNameFormProvider.pspNameLength

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      RegexpGen.from(Constraints.psaNameRegex)
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
      "abc12-d'ef *[[~& g\\/",
      FormError(fieldName, invalidKey, Seq(Constraints.inviteeNameRegex))
    )

    behave like formWithRegex(form,
      Table(
        "valid",
        Map("pspName" -> "Àtestâ 123 -'‘’")
      ),
      Table(
        "invalid",
        Map("pspName" -> "_test"),
        Map("pspName" -> "1*2~3"),
        Map("pspName" -> "{test}")
      )
    )
  }
}
