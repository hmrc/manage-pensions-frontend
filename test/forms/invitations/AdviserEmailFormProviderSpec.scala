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

import forms.behaviours.EmailBehaviours
import forms.mappings.Constraints

class AdviserEmailFormProviderSpec extends EmailBehaviours with Constraints {

  val form = new AdviserEmailFormProvider()()

  ".email" must {

    val fieldName = "email"

    val requiredKey = "messages__error__common__email__address__required"
    val maxLengthKey = "messages__error__common__email__address__length"
    val invalidKey = "messages__error__common__email__address__invalid"

    behave like formWithEmailField(form, fieldName, requiredKey, maxLengthKey, invalidKey)

    behave like formWithTransform[String](
      form,
      Map(fieldName -> " test@test.com "),
      "test@test.com"
    )
  }
}
