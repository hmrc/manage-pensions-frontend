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

package forms

import forms.mappings.Constraints
import play.api.data.FormError
import views.behaviours.StringFieldBehaviours
import wolfendale.scalacheck.regexp.RegexpGen

class PsaIdFromProviderSpec extends StringFieldBehaviours with Constraints{

  val form = new PsaIdFromProvider().apply()

  ".psaId" must {

    val fieldName = "psaId"
    val requiredKey = "messages__error__psa__id__required"
    val lengthKey = "messages__error__psa__id__length"
    val invalidKey = "messages__error__psa__id__invalid"
    val maxLength = PsaIdFromProvider.psaIdLength

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      RegexpGen.from(psaIdRegx)
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
      FormError(fieldName, invalidKey, Seq(psaIdRegx))
    )
  }
}
