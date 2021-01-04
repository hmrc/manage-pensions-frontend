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

package views.invitations.psp

import forms.invitations.psp.PspNameFormProvider
import models.CheckMode
import models.NormalMode
import models.SchemeReferenceNumber
import play.api.data.Form
import play.twirl.api.HtmlFormat
import views.behaviours.QuestionViewBehaviours
import views.html.invitations.psp.pspName

class PspNameViewSpec extends QuestionViewBehaviours[String] {

  val messageKeyPrefix = "pspName"

  val formProvider = new PspNameFormProvider()
  override val form = formProvider()

  private val pspNameView = injector.instanceOf[pspName]

  private val schemeName = "Test Scheme"

  private val returnCall = controllers.routes.SchemeDetailsController.onPageLoad(SchemeReferenceNumber("srn"))

  def createView: () => HtmlFormat.Appendable = () => pspNameView(form, NormalMode, schemeName, returnCall)(fakeRequest, messages)

  def createViewUsingForm: Form[_] => HtmlFormat.Appendable = (form: Form[_]) => pspNameView(form, CheckMode, schemeName, returnCall)(fakeRequest, messages)

  "PspName view" must {

    behave like normalPage(
      createView,
      messageKeyPrefix,
      messages(s"messages__${messageKeyPrefix}__title"),
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
