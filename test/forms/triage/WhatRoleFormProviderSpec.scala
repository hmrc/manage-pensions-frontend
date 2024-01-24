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

package forms.triage

import forms.behaviours.FormBehaviours
import models.triage.WhatRole
import models.{Field, Invalid, Required}
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.FakeRequest

class WhatRoleFormProviderSpec extends FormBehaviours {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit val messages: Messages = messagesApi.preferred(FakeRequest())

  val validData: Map[String, String] = Map(
    "value" -> WhatRole.options.head.value.get
  )

  val form = new WhatRoleFormProvider()()

  "WhatRoleFormProvider" must {

    behave like questionForm[WhatRole](WhatRole.values.head)

    behave like formWithOptionField(
      Field(
        "value",
        Required -> "messages__whatRole__error",
        Invalid -> "error.invalid"
      ),
      WhatRole.options.map(_.value.get): _*)
  }
}
