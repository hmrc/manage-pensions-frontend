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

package forms.behaviours

import forms.FormSpec
import forms.mappings.{Constraints, EmailMapping, RegexBehaviourSpec}
import play.api.data.{Form, FormError}
import views.behaviours.StringFieldBehaviours

trait EmailBehaviours extends FormSpec with StringFieldBehaviours with Constraints with RegexBehaviourSpec with EmailMapping {

  def formWithEmailField(
                          form: Form[_],
                          fieldName: String,
                          keyEmailRequired: String,
                          keyEmailLength: String,
                          keyEmailInvalid: String
                        ): Unit = {

    "behave like a form with an email field" should {
      behave like fieldThatBindsValidData(
        form,
        fieldName,
        "ab@test.com"
      )

      behave like fieldWithMaxLength(
        form,
        fieldName,
        maxLength = EmailMapping.maxLengthEmail,
        lengthError = FormError(fieldName, keyEmailLength, Seq(EmailMapping.maxLengthEmail))
      )

      behave like mandatoryField(
        form,
        fieldName,
        requiredError = FormError(fieldName, keyEmailRequired)
      )

      behave like fieldWithRegex(
        form,
        fieldName,
        "ab.com",
        FormError(fieldName, keyEmailInvalid, Seq(emailRegex))
      )

      behave like formWithRegex(
        form,
        Table(
          "valid",
          Map("email" -> "test@test.com"),
          Map("email" -> "test@t-g.com"),
          Map("email" -> "\"\"@test.com")
        ),
        Table(
          "invalid",
          Map("email" -> "@test.com"),
          Map("email" -> "test.com"),
          Map("email" -> "test@.com"),
          Map("email" -> "test@sdff"),
          Map("email" -> "test@-.com")
        )
      )
    }
  }

}
