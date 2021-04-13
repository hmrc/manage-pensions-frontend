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

package views.psp.deauthorisation.self

import forms.psp.deauthorise.RemovePspDeclarationFormProvider
import play.api.data.{Form, FormError}
import play.twirl.api.HtmlFormat
import viewmodels.Message
import views.behaviours.QuestionViewBehaviours
import views.html.psp.deauthorisation.self.declaration

class DeclarationViewSpec extends QuestionViewBehaviours[Boolean] {

  private val messageKeyPrefix = "removePspDeclaration"
  val form = new RemovePspDeclarationFormProvider()()
  private val schemeName = "Test scheme"
  private val srn = "srn"
  override val errorMessage: String = messages("messages__removePspDeclaration__required")
  override val error: FormError = FormError("value", messages("messages__removePspDeclaration__required"))

  private val view = injector.instanceOf[declaration]

  def declarationView(form: Form[Boolean] = form): () => HtmlFormat.Appendable = () =>
    view(form, schemeName, srn)(fakeRequest, messages)

  def declarationViewWithForm(form: Form[Boolean] = form): HtmlFormat.Appendable =
    view(form, schemeName, srn)(fakeRequest, messages)

  "declaration view" must {

    behave like normalPage(
      view = declarationView(),
      messageKeyPrefix = messageKeyPrefix,
      pageHeader = Message("messages__removePspDeclaration__title")
    )

    behave like pageWithBackLink(declarationView())

    behave like pageWithSubmitButton(declarationView())

    "display declaration text" in {
      val doc = asDocument(declarationView()())
      doc.getElementById("para_id").text mustBe
        messages("messages__removePspDeclaration__p_self") + " " +
          messages("messages__removePspDeclaration__p_self__screenReaderAlternative")
    }

    "show an error summary when rendered with an error" in {
      val doc = asDocument(declarationViewWithForm(form.withError(error)))
      assertRenderedById(doc, "error-summary-heading")
    }

    "show an error in the value field's label when rendered with an error" in {
      val doc = asDocument(declarationViewWithForm(form.withError(error)))
      val errorSpan = doc.getElementsByClass("error-message")
      errorSpan.text mustBe s"${messages("site.error")} ${messages(errorMessage)}"
    }
  }
}
