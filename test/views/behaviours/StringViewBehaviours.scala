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

package views.behaviours

import play.api.data.Form
import play.api.data.FormError
import play.twirl.api.HtmlFormat

trait StringViewBehaviours extends QuestionViewBehaviours[String] {

  val answer = "answer"
  def stringPage(createView: (Form[String]) => HtmlFormat.Appendable,
                 messageKeyPrefix: String,
                 expectedFormAction: String,
                 label: Option[String] = None,
                 expectedHint: Option[String] = None,
                 id: String = "value"): Unit = {

    "behave like a page with a string value field" when {
      "rendered" must {

        "contain a label for the value" in {
          val doc = asDocument(createView(form))
          val expectedHintText = expectedHint.map(messages(_))
          assertContainsLabel(doc, id, messages(label.getOrElse(s"messages__${messageKeyPrefix}__title")), expectedHintText)
        }

        "contain an input for the value" in {
          val doc = asDocument(createView(form))
          assertRenderedById(doc, id)
        }
      }

      "rendered with a valid form" must {
        "include the form's value in the value input" in {
          val doc = asDocument(createView(form.fill(answer)))
          doc.getElementById(id).attr("value") mustBe answer
        }
      }

      "rendered with an error" must {

        "show an error summary" in {
          val doc = asDocument(createView(form.withError(error)))
          assertRenderedById(doc, "error-summary-heading")
        }

        "show an error in the value field's label" in {
          val error = FormError(id, errorMessage)
          val doc = asDocument(createView(form.withError(error)))
          val errorSpan = doc.getElementsByClass("error-notification").first
          errorSpan.text mustBe s"${messages("site.error")} ${messages(errorMessage)}"
        }
      }
    }
  }
}
