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

import forms.invitations.PsaIdFormProvider
import models.NormalMode
import play.api.data.Form
import play.twirl.api.HtmlFormat
import views.behaviours.QuestionViewBehaviours
import views.html.invitations.psaId

class PsaIdViewSpec extends QuestionViewBehaviours[String] {

  val messageKeyPrefix = "psa__id"

  val formProvider = new PsaIdFormProvider()
  override val form = formProvider()

  private val psaIdView = injector.instanceOf[psaId]

  def createView: () => HtmlFormat.Appendable = () => psaIdView(form, "psaName", NormalMode)(fakeRequest, messages)

  def createViewUsingForm: Form[_] => HtmlFormat.Appendable = (form: Form[_]) => psaIdView(form, "psaName", NormalMode)(fakeRequest, messages)

  "PsaId view" must {

    behave like normalPage(
      createView,
      messageKeyPrefix,
      messages(s"messages__${messageKeyPrefix}__heading",  "psaName")
    )

    behave like pageWithBackLink(createView)

    behave like pageWithTextFields(
      createViewUsingForm,
      messageKeyPrefix,
      controllers.invitations.routes.PsaNameController.onSubmit(NormalMode).url,
      "psaId"
    )
  }
}
