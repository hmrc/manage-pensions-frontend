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

package views.invitations.psp

import forms.invitations.psp.PspNameFormProvider
import models.{CheckMode, NormalMode}
import play.api.data.Form
import play.twirl.api.HtmlFormat
import views.behaviours.QuestionViewBehaviours
import views.html.invitations.psp.pspName

class PspNameViewSpec extends QuestionViewBehaviours[String] {

  val messageKeyPrefix = "pspName"

  val formProvider = new PspNameFormProvider()
  override val form = formProvider()

  private val pspNameView = injector.instanceOf[pspName]

  def createView: () => HtmlFormat.Appendable = () => pspNameView(form, NormalMode)(fakeRequest, messages)

  def createViewUsingForm: Form[_] => HtmlFormat.Appendable = (form: Form[_]) => pspNameView(form, CheckMode)(fakeRequest, messages)

  "PspName view" must {

    behave like normalPage(
      createView,
      messageKeyPrefix,
      messages(s"messages__${messageKeyPrefix}__heading"),
      s"_p1"
    )

    behave like pageWithBackLink(createView)

    behave like pageWithErrorOutsideLabel(
      createViewUsingForm,
      messageKeyPrefix,
      controllers.invitations.routes.PsaNameController.onSubmit(NormalMode).url,
      "pspName"
    )
  }
}
