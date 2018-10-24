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

package forms.invitations

import forms.mappings.Constraints
import play.api.data.FormError
import views.behaviours.StringFieldBehaviours
import wolfendale.scalacheck.regexp.RegexpGen

class PsaNameFormProviderSpec extends StringFieldBehaviours with Constraints{

  val formProvider = new PsaNameFormProvider()
  val form = formProvider()

  ".psaName" must {

    val fieldName = "psaName"
    val requiredKey = "messages__error__psa__name__required"
    val lengthKey = "messages__error__psa__name__length"
    val invalidKey = "messages__error__psa__name__invalid"
    val maxLength = PsaNameFormProvider.psaNameLength

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
      "abc12-d'ef *& g\\/",
      FormError(fieldName, invalidKey, Seq(Constraints.psaNameRegex))
    )

    behave like formWithRegex(form,
      Table(
        "valid",
        Map("psaName" -> "Àtestâ 123 -'‘’")
      ),
      Table(
        "invalid",
        Map("psaName" -> "_test"),
        Map("psaName" -> "1*23"),
        Map("psaName" -> "{test}")
      )
    )
  }
}
