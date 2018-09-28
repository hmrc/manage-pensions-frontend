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

class AdviserDetailsFormProviderSpec extends StringFieldBehaviours with Constraints {

  val form = new AdviserDetailsFormProvider()()

  ".adviserName" must {

    val fieldName = "adviserName"
    val requiredKey = "messages__error__adviser__name__required"
    val maxLenghtErrorKey = "messages__error__adviser__name__length"
    val invalidErrorKey = "messages__error__adviser__name__invalid"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      RegexpGen.from(Constraints.nameRegex)
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )

    behave like fieldWithMaxLength(
      form,
      fieldName,
      AdviserDetailsFormProvider.adviserNameLength,
      FormError(fieldName, maxLenghtErrorKey, Seq(AdviserDetailsFormProvider.adviserNameLength))
    )

    behave like fieldWithRegex(
      form,
      fieldName,
      "1234",
      FormError(fieldName, invalidErrorKey, Seq(Constraints.nameRegex))
    )

    behave like formWithTransform[String](
      form,
      Map(fieldName -> " test "),
      "test"
    )

    behave like formWithRegex(form,
      Table(
        "valid",
        Map("adviserName" -> "Àtestâ -'‘’")
      ),
      Table(
        "invalid",
        Map("adviserName" -> "_test"),
        Map("adviserName" -> "123"),
        Map("adviserName" -> "{test}")
      )
    )
  }
}
