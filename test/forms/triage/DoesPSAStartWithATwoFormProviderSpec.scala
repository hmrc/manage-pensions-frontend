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

package forms.triage

import forms.behaviours.FormBehaviours
import models.triage.DoesPSAStartWithATwo
import models.Field
import models.Invalid
import models.Required
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.Messages
import play.api.i18n.MessagesApi
import play.api.test.FakeRequest

class DoesPSAStartWithATwoFormProviderSpec extends FormBehaviours with GuiceOneAppPerSuite {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit val messages: Messages = messagesApi.preferred(FakeRequest())

  val validData: Map[String, String] = Map(
    "value" -> DoesPSAStartWithATwo.options.head.value
  )

  val form = new DoesPSAStartWithATwoFormProvider()()

  "DoesPSAStartWithATwoFormProvider" must {

    behave like questionForm[DoesPSAStartWithATwo](DoesPSAStartWithATwo.values.head)

    behave like formWithOptionField(
      Field(
        "value",
        Required -> messages("messages__doesPSAStartWithATwo__error__required"),
        Invalid -> "error.invalid"
      ),
      DoesPSAStartWithATwo.options.map(_.value): _*)
  }
}
