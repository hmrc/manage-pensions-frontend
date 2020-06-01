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

package views.triage

import forms.triage.DoesPSTRStartWithTwoFormProvider
import play.api.data.Form
import views.behaviours.YesNoViewBehaviours
import views.html.triage.doesPSTRStartWithTwo

class DoesPSTRStartWithTwoViewSpec extends YesNoViewBehaviours {

  private val messageKeyPrefix = "doesPSTRStartWithTwo"
  private val postCall = controllers.triage.routes.DoesPSTRStartWithTwoController.onPageLoad()

  val formProvider = new DoesPSTRStartWithTwoFormProvider
  val form: Form[Boolean] = formProvider()

  private val doesPSTRStartWithTwoView = injector.instanceOf[doesPSTRStartWithTwo]

  private def createView() =
    () => doesPSTRStartWithTwoView(
      form,
      postCall
    )(fakeRequest, messages)

  private def createViewUsingForm =
    (form: Form[_]) => doesPSTRStartWithTwoView(
      form,
      postCall
    )(fakeRequest, messages)

  "DoesPSTRStartWithTwoView" must {
    behave like normalPageWithTitle(createView(), messageKeyPrefix, messages(s"messages__${messageKeyPrefix}__title"),
      messages(s"messages__${messageKeyPrefix}__title"))

    behave like pageWithSubmitButton(createView())

    behave like yesNoPageExplicitLegend(createView = createViewUsingForm, messageKeyPrefix = messageKeyPrefix,
      expectedFormAction = "/",
      legend = messages(s"messages__${messageKeyPrefix}__title"))
  }
}

