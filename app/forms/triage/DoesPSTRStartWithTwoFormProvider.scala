/*
 * Copyright 2022 HM Revenue & Customs
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

package forms.triage

import forms.mappings.Mappings
import javax.inject.Inject
import models.triage.DoesPSTRStartWithATwo
import play.api.data.Form
import play.api.i18n.Messages

class DoesPSTRStartWithTwoFormProvider @Inject() extends Mappings {

  def apply(errorKey: String = "messages__doesPSTRStartWithTwo__error__required")(implicit messages: Messages): Form[DoesPSTRStartWithATwo] =
    Form(
      "value" -> enumerable[DoesPSTRStartWithATwo](messages(errorKey))
    )
}
