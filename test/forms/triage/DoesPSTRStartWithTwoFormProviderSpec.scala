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
import models.triage.DoesPSTRStartWithATwo
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.Messages
import play.api.i18n.MessagesApi
import play.api.test.FakeRequest
import models.Field
import models.Invalid
import models.Required

class DoesPSTRStartWithTwoFormProviderSpec extends FormBehaviours with GuiceOneAppPerSuite {

  private val hint = Some("opt1")

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit val messages: Messages = messagesApi.preferred(FakeRequest())

  val validData: Map[String, String] = Map(
    "value" -> DoesPSTRStartWithATwo.options(hint).head.value
  )

  private val requiredKey = messages("messages__doesPSTRStartWithTwo__error__required")

  val formProvider = new DoesPSTRStartWithTwoFormProvider()
  val form = formProvider()

  ".value" must {

    val fieldName = "value"

    behave like questionForm[DoesPSTRStartWithATwo](DoesPSTRStartWithATwo.values.head)

    behave like formWithOptionField(
      Field(
        fieldName,
        Required -> requiredKey,
        Invalid -> "error.invalid"
      ),
      DoesPSTRStartWithATwo.options(hint).map(_.value): _*)

  }

}

