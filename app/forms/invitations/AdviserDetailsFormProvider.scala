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

import forms.mappings.{Mappings, Transforms}
import javax.inject.Inject
import play.api.data.Form

class AdviserDetailsFormProvider @Inject() extends Mappings with Transforms {

  import AdviserDetailsFormProvider._

  def apply(): Form[String] = Form(
    "adviserName" -> text(requiredKey).transform(
      standardTextTransform,
      noTransform
    ).verifying(
      firstError(
        maxLength(AdviserDetailsFormProvider.adviserNameLength, maxLengthKey),
        adviserName(invalidKey)
      )
    )
  )
}

object AdviserDetailsFormProvider {
  val adviserNameLength = 107

  val requiredKey = "messages__error__adviser__name__required"
  val maxLengthKey = "messages__error__adviser__name__length"
  val invalidKey = "messages__error__adviser__name__invalid"
}

