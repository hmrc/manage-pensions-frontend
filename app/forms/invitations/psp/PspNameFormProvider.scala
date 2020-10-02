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

package forms.invitations.psp

import forms.mappings.Mappings
import forms.mappings.Transforms
import javax.inject.Inject
import play.api.data.Form

class PspNameFormProvider @Inject() extends Mappings with Transforms {
  def apply(): Form[String] = Form(
    "pspName" -> text("messages__error__pspName__required").
      transform(standardTextTransform, noTransform).
      verifying(firstError(
        maxLength(PspNameFormProvider.pspNameLength, "messages__error__pspName__length"),
        psaName("messages__error__pspName__invalid")))
  )
}

object PspNameFormProvider {
  val pspNameLength = 107
}
