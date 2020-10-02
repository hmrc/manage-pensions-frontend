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

import forms.invitations.DeclarationFormProvider
import play.api.data.Form
import play.api.data.FormError
import play.twirl.api.HtmlFormat
import viewmodels.Message
import views.behaviours.QuestionViewBehaviours
import views.html.invitations.psp.declaration

class DeclarationViewSpec extends QuestionViewBehaviours[Boolean] {

  val messageKeyPrefix = "psp_declaration"
  val form = new DeclarationFormProvider()()
  override val errorMessage: String = messages("messages__error__psp_declaration__required")
  override val error: FormError = FormError("agree", errorMessage)

  private val declaration = injector.instanceOf[declaration]

  def declarationView(): () => HtmlFormat.Appendable = () => declaration(form)(fakeRequest, messages)

  private def declarationViewWithForm(form: Form[_]) = declaration(form)(fakeRequest, messages)

  "declaration view" must {

    behave like normalPage(declarationView(), messageKeyPrefix, Message("messages__psp_declaration__title"))

    behave like pageWithBackLink(declarationView())

    behave like pageWithSubmitButton(declarationView())

    "show an error summary when rendered with an error" in {
      val doc = asDocument(declarationViewWithForm(form.withError(error)))
      assertRenderedById(doc, "error-summary-heading")
    }

    "show an error in the value field's label when rendered with an error" in {
      val doc = asDocument(declarationViewWithForm(form.withError(error)))
      val errorSpan = doc.getElementsByClass("error-message")
      errorSpan.text mustBe s"${messages("site.error")} ${messages(errorMessage)}"
    }

    "display the declaration" in {
      asDocument(declarationView()()) must haveDynamicText("messages__psp_declaration__continue")
    }

    "display the statement1 declaration" in {
      asDocument(declarationView()()) must haveDynamicText("messages__psp_declaration__statement1")
    }

    "display the statement2 declaration" in {
      asDocument(declarationView()()) must haveDynamicText("messages__psp_declaration__statement2")
    }

    "display the statement3 declaration" in {
      asDocument(declarationView()()) must haveDynamicText("messages__psp_declaration__statement3")
    }
  }

}
