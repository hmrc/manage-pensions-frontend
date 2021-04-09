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

package views.remove

import forms.remove.psp.RemovePspDeclarationFormProvider
import play.api.data.{Form, FormError}
import play.twirl.api.HtmlFormat
import viewmodels.Message
import views.behaviours.QuestionViewBehaviours
import views.html.remove.psa.psaRemovePspDeclaration

class PsaRemovePspDeclarationViewSpec extends QuestionViewBehaviours[Boolean] {

  private val messageKeyPrefix = "removePspDeclaration"
  val form = new RemovePspDeclarationFormProvider()()
  private val schemeName = "Test scheme"
  private val srn = "srn"
  override val errorMessage: String = messages("messages__removePspDeclaration__required")
  override val error: FormError = FormError("value", messages("messages__removePspDeclaration__required"))

  private val view = injector.instanceOf[psaRemovePspDeclaration]

  def psaRemovePspDeclarationView(form: Form[Boolean] = form): () => HtmlFormat.Appendable = () =>
    view(form, schemeName, srn, 0)(fakeRequest, messages)

  def psaRemovePspDeclarationViewWithForm(form: Form[Boolean] = form): HtmlFormat.Appendable =
    view(form, schemeName, srn, 0)(fakeRequest, messages)

  "declaration view" must {

    behave like normalPage(
      view = psaRemovePspDeclarationView(),
      messageKeyPrefix = messageKeyPrefix,
      pageHeader = Message("messages__removePspDeclaration__title")
    )

    behave like pageWithBackLink(psaRemovePspDeclarationView())

    behave like pageWithSubmitButton(psaRemovePspDeclarationView())

    "display declaration text" in {
      val doc = asDocument(psaRemovePspDeclarationView()())
      doc.getElementById("para_id").text mustBe
        messages("messages__removePspDeclaration__p") + " " +
          messages("messages__removePspDeclaration__p__screenReaderAlternative")
    }

    "show an error summary when rendered with an error" in {
      val doc = asDocument(psaRemovePspDeclarationViewWithForm(form.withError(error)))
      assertRenderedById(doc, "error-summary-heading")
    }

    "show an error in the value field's label when rendered with an error" in {
      val doc = asDocument(psaRemovePspDeclarationViewWithForm(form.withError(error)))
      val errorSpan = doc.getElementsByClass("error-message")
      errorSpan.text mustBe s"${messages("site.error")} ${messages(errorMessage)}"
    }
  }
}
