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

import play.twirl.api.HtmlFormat
import viewmodels._
import views.ViewSpecBase

trait CheckYourAnswersBehaviours extends ViewSpecBase {

  // scalastyle:off method.length
  def checkYourAnswersPage(view: (Seq[Section]) => HtmlFormat.Appendable): Unit = {
    "behave like a Check your Answers page" when {
      "there are answers to render" must {
        "correctly display an AnswerSection" in {
          val headingKey = "test-headingKey"
          val answer1 = "test-answer-1"
          val answer2 = "test-answer-2"

          val answerRow = AnswerRow("test-label", Seq(answer1, answer2), answerIsMessageKey = false, "http//:google.com")

          val section = AnswerSection(
            Some(headingKey),
            Seq(
              answerRow
            )
          )

          val doc = asDocument(view(Seq(section)))

          assertRenderedByIdWithText(doc, "cya-0-heading", headingKey)
          assertRenderedByIdWithText(doc, "cya-0-0-question", answerRow.label)
          assertRenderedByIdWithText(doc, "cya-0-0-0-answer", answer1)
          assertRenderedByIdWithText(doc, "cya-0-0-1-answer", answer2)
          assertLink(doc, "cya-0-0-change", answerRow.changeUrl.get)
        }

        "correctly display an AnswerSection where answerIsMessageKey is true" in {
          val headingKey = "test-headingKey"

          val answerRow = AnswerRow("test-label", Seq("site.yes"), answerIsMessageKey = true, "http//:google.com")

          val section = AnswerSection(
            Some(headingKey),
            Seq(
              answerRow
            )
          )

          val doc = asDocument(view(Seq(section)))

          assertRenderedByIdWithText(doc, "cya-0-0-0-answer", "Yes")
        }

        "correctly display a RepeaterAnswerSection" in {
          val relevanceRow = AnswerRow(
            "test-relevance-row-label",
            Seq("test-relevance-row-answer"),
            answerIsMessageKey = false,
            "test-relevance-row-url"
          )

          val answerRow = RepeaterAnswerRow(
            "test-answer",
            "test-change-url",
            "test-delete-url"
          )

          val section = RepeaterAnswerSection("test-headingKey", relevanceRow, Seq(answerRow), "test-add-link-key", "test-add-link-url")

          val doc = asDocument(view(Seq(section)))

          assertRenderedByIdWithText(doc, "cya-0-heading", section.headingKey)

          assertRenderedByIdWithText(doc, "cya-0-relevance-question", relevanceRow.label)
          assertRenderedByIdWithText(doc, "cya-0-relevance-0-answer", relevanceRow.answer.head)
          assertLink(doc, "cya-0-relevance-change", relevanceRow.changeUrl.get)

          assertRenderedByIdWithText(doc, "cya-0-0-answer", answerRow.answer)
          assertLink(doc, "cya-0-0-change", answerRow.changeUrl)
          assertLink(doc, "cya-0-0-delete", answerRow.deleteUrl)

          assertLink(doc, "cya-0-add", section.addLinkUrl)
        }

        "display the correct number of answers" in {
          val answerRow = AnswerRow("test-label", Seq("test-answer"), answerIsMessageKey = true, "test-change-url")

          val section = AnswerSection(
            Some("test-heading-key"),
            Seq(
              answerRow
            )
          )

          val doc = asDocument(view(Seq(section, section)))

          assertRenderedById(doc, "cya-0-heading")
          assertRenderedById(doc, "cya-1-heading")
          assertNotRenderedById(doc, "cya-2-heading")
        }
      }
    }
  }

}
