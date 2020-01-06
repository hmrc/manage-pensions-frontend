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

package views.invitations

import forms.invitations.AdviserEmailFormProvider
import models.NormalMode
import play.api.data.Form
import play.twirl.api.HtmlFormat
import views.behaviours.QuestionViewBehaviours
import views.html.invitations.adviserEmailAddress

class AdviserEmailAddressViewSpec extends QuestionViewBehaviours[String] {

  val messageKeyPrefix = "adviser__email__address"
  val form = new AdviserEmailFormProvider().apply()
  val adviserName = "test adviser"
  private val adviserEmailAddressView = injector.instanceOf[adviserEmailAddress]

  private val createView: () => HtmlFormat.Appendable = () => adviserEmailAddressView(form, NormalMode, adviserName)(fakeRequest, messages)

  private val createViewWithForm: Form[String] => HtmlFormat.Appendable =
    (form: Form[String]) => adviserEmailAddressView(form, NormalMode, adviserName)(fakeRequest, messages)

  behave like normalPage(createView, messageKeyPrefix,
    messages("messages__adviser__email__address__heading", adviserName),
    "_p1")

  behave like pageWithBackLink(createView)

  behave like pageWithTextFields(
    createViewWithForm,
    messageKeyPrefix,
    controllers.invitations.routes.AdviserDetailsController.onSubmit(NormalMode).url,
    "email"
  )

  behave like pageWithSubmitButton(createView)

}
