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

package forms.invitations

import forms.behaviours.CheckboxBehaviour
import play.api.data.Form

class DeclarationFormProviderSpec extends CheckboxBehaviour {

  private val form: Form[Boolean] = new DeclarationFormProvider()()
  private val fieldName = "agree"
  private val trueValue = "agreed"
  private val invalidKey = "messages__error__declaration__required"

  "DeclarationFormProvider" should {
    behave like formWithCheckbox(form, fieldName, trueValue, acceptTrueOnly = true, invalidKey)
  }

}
