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

package views.invitations

import controllers.invitations.psa.routes._
import forms.invitations.psa.PsaNameFormProvider
import models.CheckMode
import models.NormalMode
import play.api.data.Form
import play.twirl.api.HtmlFormat
import views.behaviours.QuestionViewBehaviours
import views.html.invitations.psa.psaName

class PsaNameViewSpec extends QuestionViewBehaviours[String] {

  val messageKeyPrefix = "psa__name"

  val formProvider = new PsaNameFormProvider()
  override val form = formProvider()

  private val psaNameView = injector.instanceOf[psaName]

  def createView: () => HtmlFormat.Appendable = () => psaNameView(form, NormalMode)(fakeRequest, messages)

  def createViewUsingForm: Form[_] => HtmlFormat.Appendable = (form: Form[_]) => psaNameView(form, CheckMode)(fakeRequest, messages)

  "PsaName view" must {

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
      PsaNameController.onSubmit(NormalMode).url,
      "psaName"
    )
  }
}
