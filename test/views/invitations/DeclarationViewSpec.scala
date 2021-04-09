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

import forms.invitations.psa.DeclarationFormProvider
import play.api.data.Form
import play.api.data.FormError
import play.twirl.api.HtmlFormat
import viewmodels.Message
import views.behaviours.QuestionViewBehaviours
import views.html.invitations.psa.declaration

class DeclarationViewSpec extends QuestionViewBehaviours[Boolean] {

  val messageKeyPrefix = "declaration"
  val form = new DeclarationFormProvider()()
  override val errorMessage = messages("messages__error__declaration__required")
  override val error = FormError("agree", messages("messages__error__declaration__required"))

  private val declarationView = injector.instanceOf[declaration]

  def declarationView(haveWorkingKnowledge: Boolean = false, isMasterTrust: Boolean = false): () => HtmlFormat.Appendable = () =>
    declarationView(haveWorkingKnowledge, isMasterTrust, form)(fakeRequest, messages)

  private def declarationViewWithForm(form: Form[_]) =
    declarationView(haveWorkingKnowledge = true, isMasterTrust = false, form)(fakeRequest, messages)

  "declaration view" must {

    behave like normalPage(declarationView(), messageKeyPrefix, Message("messages__declaration__title"))

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
      asDocument(declarationView()()) must haveDynamicText("messages__declaration__continue")
    }

    "display the statement1 declaration" in {
      asDocument(declarationView()()) must haveDynamicText("messages__declaration__statement1")
    }

    "display the statement2 declaration" in {
      asDocument(declarationView()()) must haveDynamicText("messages__declaration__statement2")
    }

    "display the statement3 declaration" in {
      asDocument(declarationView()()) must haveDynamicText("messages__declaration__statement3")
    }

    "display the statement4 declaration" in {
      asDocument(declarationView()()) must haveDynamicText("messages__declaration__statement4")
    }

    "have statement5 fit and proper declaration if no adviser" in {
      val document = asDocument(declarationView(haveWorkingKnowledge = true)())
      document must haveDynamicText("messages__declaration__statement5__no__adviser")
      document must not(haveDynamicText("messages__declaration__statement5__with__adviser"))
    }

    "have statement5 no working knowledge declaration if have an adviser" in {
      val document = asDocument(declarationView()())
      document must haveDynamicText("messages__declaration__statement5__with__adviser")
      document must not(haveDynamicText("messages__declaration__statement5__no__adviser"))
    }

    "display the statement6 declaration" in {
      asDocument(declarationView()()) must haveDynamicText("messages__declaration__statement6")
    }

    "have statement7 master trust declaration if master trust" in {
      val document = asDocument(declarationView(isMasterTrust = true)())
      document must haveDynamicText("messages__declaration__statement7")
    }

    "not have statement7 master trust declaration if not master trust" in {
      val document = asDocument(declarationView()())
      document must not(haveDynamicText("messages__declaration__statement7"))
    }
  }

}
