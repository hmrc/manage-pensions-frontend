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

package forms.behaviours

import forms.FormSpec
import forms.mappings.Constraints
import forms.mappings.EmailMapping
import forms.mappings.RegexBehaviourSpec
import play.api.data.Form
import play.api.data.FormError
import views.behaviours.StringFieldBehaviours

trait EmailBehaviours extends FormSpec with StringFieldBehaviours with Constraints with RegexBehaviourSpec with EmailMapping {

  val keyNoAtSignIncluded: String = "messages__error__common__email__no_at_sign"
  val keyStartsWithAtSign: String = "messages__error__common__email__start_with_at_sign"
  val keyDotAfterAtSign: String = "messages__error__common__email__dot_after_at_sign"
  val keyEndsWithDot: String = "messages__error__common__email__ends_with_dot"

  //scalastyle:off method.length
  def formWithEmailField(
                          form: Form[?],
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

      behave like emailWithCorrectFormat(
        form,
        fieldName
      )

      behave like fieldWithRegex(
        form,
        fieldName,
        "test@com",
        FormError(fieldName, keyEmailInvalid, Seq(Constraints.emailRegex))
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
          Map("email" -> "test@sdff"),
          Map("email" -> "test@-.com")
        )
      )
    }
  }

  private def emailWithCorrectFormat(
                                      form: Form[?],
                                      fieldName: String
                                    ): Unit = {

    def result(value: String) = form.bind(Map(fieldName -> value)).apply(fieldName)

    "not bind when @ sign not included" in {
      result("test.com").errors shouldEqual Seq(FormError(fieldName, keyNoAtSignIncluded))
    }

    "not bind when starts with @ sign" in {
      result("@test.com").errors shouldEqual Seq(FormError(fieldName, keyStartsWithAtSign))
    }

    "not bind when nothing is between @ sign and dot(.)" in {
      result("test@.com").errors shouldEqual Seq(FormError(fieldName, keyDotAfterAtSign))
    }

    "not bind when ends with a dot(.)" in {
      result("test@com.").errors shouldEqual Seq(FormError(fieldName, keyEndsWithDot))
    }
  }

}
