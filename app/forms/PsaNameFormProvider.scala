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

import javax.inject.Inject

import forms.mappings.Mappings
import models.PsaName
import play.api.data.Form
import play.api.data.Forms._


class PsaNameFormProvider @Inject() extends Mappings {
  val psaNameMaxLength = 107
  def apply(): Form[PsaName] = Form(mapping(
    "psaName" -> text(
      "messages__error__psa__name").
      verifying(firstError(
        maxLength(psaNameMaxLength, "messages__error__psa__name_length"),
        safeText("messages__error__psa__name_invalid")))
  )(PsaName.apply)(PsaName.unapply))
}
