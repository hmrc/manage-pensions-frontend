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

import forms.invitations.psp.PspIdFormProvider
import models.{NormalMode, SchemeReferenceNumber}
import play.api.data.Form
import play.twirl.api.HtmlFormat
import views.behaviours.QuestionViewBehaviours
import views.html.invitations.psp.pspId

class PspIdViewSpec extends QuestionViewBehaviours[String] {

  val messageKeyPrefix = "pspId"

  val formProvider = new PspIdFormProvider()
  override val form = formProvider()

  private val pspIdView = injector.instanceOf[pspId]

  private val schemeName = "Test Scheme"

  private val returnCall = controllers.routes.SchemeDetailsController.onPageLoad(SchemeReferenceNumber("srn"))

  def createView: () => HtmlFormat.Appendable = () => pspIdView(form, "pspName", NormalMode, schemeName, returnCall)(fakeRequest, messages)

  def createViewUsingForm: Form[_] => HtmlFormat.Appendable = (form: Form[_]) => pspIdView(form, "pspName", NormalMode, schemeName, returnCall)(fakeRequest, messages)

  "PspId view" must {

    behave like normalPage(
      createView,
      messageKeyPrefix,
      messages(s"messages__${messageKeyPrefix}__heading",  "pspName")
    )

    behave like pageWithBackLink(createView)

    behave like pageWithErrorOutsideLabel(
      createViewUsingForm,
      messageKeyPrefix,
      controllers.invitations.psp.routes.PspIdController.onSubmit(NormalMode).url,
      "pspId"
    )
  }
}
