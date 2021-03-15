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

package forms.triage

import forms.behaviours.FormBehaviours
import models.triage.WhatDoYouWantToDo
import models.Field
import models.Invalid
import models.Required

class WhatDoYouWantToDoFormProviderSpec extends FormBehaviours {

  val validData: Map[String, String] = Map(
    "value" -> WhatDoYouWantToDo.options("PSA").head.value
  )

  val form = new WhatDoYouWantToDoFormProvider()("PSA")

  "WhatDoYouWantToDoFormProvider" must {

    behave like questionForm[WhatDoYouWantToDo](WhatDoYouWantToDo.values("PSA").head)

    behave like formWithOptionField(
      Field(
        "value",
        Required -> "messages__whatDoYouWantToDo__error__required",
        Invalid -> "error.invalid"
      ),
      WhatDoYouWantToDo.options("PSA").map(_.value): _*)
  }
}
