/*
 * Copyright 2019 HM Revenue & Customs
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

import javax.inject.Inject

import forms.mappings.{Mappings, Transforms}
import play.api.data.Form


class PsaIdFormProvider @Inject() extends Mappings with Transforms {
  def apply(): Form[String] = Form(
    "psaId" -> text("messages__error__psa__id__required").
      transform(noSpaceWithUpperCaseTransform, noTransform).
      verifying(firstError(
        maxLength(PsaIdFormProvider.psaIdLength, "messages__error__psa__id__invalid"),
        psaId("messages__error__psa__id__invalid")))
  )
}

object PsaIdFormProvider {
  val psaIdLength = 8
}
