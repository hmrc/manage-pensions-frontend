/*
 * Copyright 2021 HM Revenue & Customs
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

package forms.invitations.psa

import com.google.inject.Inject
import forms.mappings.CheckboxMapping
import play.api.data.Form

class DeclarationFormProvider @Inject() () extends CheckboxMapping {

  private val fieldName = "consent"
  private val trueValue = "true"
  private val invalidKey = "messages__error__declaration__required"

  def apply(): Form[Boolean] =
    Form(
      fieldName -> checkboxMapping(fieldName, trueValue, acceptTrueOnly = true, invalidKey)
    )

}
