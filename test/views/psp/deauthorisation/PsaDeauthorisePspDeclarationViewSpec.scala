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

package views.psp.deauthorisation

import forms.psp.deauthorise.DeauthorisePspDeclarationFormProvider
import play.api.data.{Form, FormError}
import play.twirl.api.HtmlFormat
import viewmodels.Message
import views.behaviours.QuestionViewBehaviours
import views.html.psp.deauthorisation.psaDeauthorisePspDeclaration

class PsaDeauthorisePspDeclarationViewSpec extends QuestionViewBehaviours[Boolean] {

  private val messageKeyPrefix = "deauthPspDeclaration"
  val form = new DeauthorisePspDeclarationFormProvider()()
  private val schemeName = "Test scheme"
  private val srn = "srn"
  override val errorMessage: String = messages("messages__deauthPspDeclaration__required")
  override val error: FormError = FormError("value", messages("messages__deauthPspDeclaration__required"))

  private val view = injector.instanceOf[psaDeauthorisePspDeclaration]

  def psaDeauthPspDeclarationView(form: Form[Boolean] = form): () => HtmlFormat.Appendable = () =>
    view(form, schemeName, srn, 0)(fakeRequest, messages)

  def psaDeauthPspDeclarationViewWithForm(form: Form[Boolean] = form): HtmlFormat.Appendable =
    view(form, schemeName, srn, 0)(fakeRequest, messages)

  "declaration view" must {

    behave like normalPage(
      view = psaDeauthPspDeclarationView(),
      messageKeyPrefix = messageKeyPrefix,
      pageHeader = Message("messages__deauthPspDeclaration__title")
    )

    behave like pageWithBackLink(psaDeauthPspDeclarationView())

    behave like pageWithSubmitButton(psaDeauthPspDeclarationView())

    "display declaration text" in {
      val doc = asDocument(psaDeauthPspDeclarationView()())
      doc.getElementById("para_id").text mustBe
        messages("messages__deauthPspDeclaration__p") + " " +
          messages("messages__deauthPspDeclaration__p__screenReaderAlternative")
    }

    "show an error summary when rendered with an error" in {
      val doc = asDocument(psaDeauthPspDeclarationViewWithForm(form.withError(error)))
      assertRenderedById(doc, "error-summary-heading")
    }

    "show an error in the value field's label when rendered with an error" in {
      val doc = asDocument(psaDeauthPspDeclarationViewWithForm(form.withError(error)))
      val errorSpan = doc.getElementsByClass("error-message")
      errorSpan.text mustBe s"${messages("site.error")} ${messages(errorMessage)}"
    }
  }
}
