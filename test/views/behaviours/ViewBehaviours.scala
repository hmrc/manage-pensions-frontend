/*
 * Copyright 2019 HM Revenue & Customs
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

import java.time.LocalDate

import org.jsoup.Jsoup
import play.api.data.{Form, FormError}
import play.api.i18n.Lang
import play.twirl.api.HtmlFormat
import views.ViewSpecBase

trait ViewBehaviours extends ViewSpecBase {
 implicit val lang = Lang("en")
  def normalPageWithTitle(view: () => HtmlFormat.Appendable,
                          messageKeyPrefix: String,
                          title: String,
                          pageHeader: String,
                          expectedGuidanceKeys: String*): Unit = {

    "behave like a normal page" when {
      "rendered" must {
        "have the correct banner title" in {
          val doc = asDocument(view())
          val nav = doc.getElementById("proposition-menu")
          val span = nav.children.first
          span.text mustBe messagesApi("site.service_name")
        }

        "display the correct browser title" in {
          val doc = asDocument(view())
          val titleKey = if(title == messagesApi("messages__schemesOverview__title")){
            messagesApi("messages__manage_pension_schemes__title")
          } else {
            title + " - " + messagesApi("messages__manage_pension_schemes__title")}
          assertEqualsMessage(doc, "title", titleKey)
        }

        "display the correct page title" in {
          val doc = asDocument(view())
          assertPageTitleEqualsMessage(doc, pageHeader)
        }

        "display the correct guidance" in {
          val doc = asDocument(view())
          for (key <- expectedGuidanceKeys) assertContainsText(doc, messages(s"messages__${messageKeyPrefix}_$key"))
        }
      }
    }

  }

  def normalPageWithoutBrowserTitle(view: () => HtmlFormat.Appendable,
                          messageKeyPrefix: String,
                          pageHeader: String,
                          expectedGuidanceKeys: String*): Unit = {

    "behave like a normal page" when {
      "rendered" must {
        "have the correct banner title" in {
          val doc = asDocument(view())
          val nav = doc.getElementById("proposition-menu")
          val span = nav.children.first
          span.text mustBe messagesApi("site.service_name")
        }

        "display the correct page title" in {
          val doc = asDocument(view())
          assertPageTitleEqualsMessage(doc, pageHeader)
        }

        "display the correct guidance" in {
          val doc = asDocument(view())
          for (key <- expectedGuidanceKeys) assertContainsText(doc, messages(s"messages__${messageKeyPrefix}_$key"))
        }
      }
    }

  }

  def normalPage(view: () => HtmlFormat.Appendable,
                 messageKeyPrefix: String,
                 pageHeader: String,
                 expectedGuidanceKeys: String*): Unit = {

    normalPageWithTitle(
      view,
      messageKeyPrefix,
      messagesApi(s"messages__${messageKeyPrefix}__title"),
      pageHeader,
      expectedGuidanceKeys: _*
    )
  }

  def pageWithBackLink(view: () => HtmlFormat.Appendable): Unit = {

    "behave like a page with a back link" must {
      "have a back link" in {
        val doc = asDocument(view())
        assertRenderedById(doc, "back-link")
      }
    }
  }

  def pageWithReturnLink(view: () => HtmlFormat.Appendable, url: String, text: String): Unit = {

    "behave like a page with a return link" must {
      "have a return link" in {
        val doc = asDocument(view())
        assertRenderedByIdWithText(doc, "return-link", text)
        assertLink(doc, "return-link", url)
      }
    }
  }


  def pageWithSecondaryHeader(view: () => HtmlFormat.Appendable,
                              heading: String): Unit = {

    "behave like a page with a secondary header" in {
      Jsoup.parse(view().toString()).getElementsByClass("heading-secondary").text() must include(heading)
    }
  }

  def pageWithSubmitButton(view: () => HtmlFormat.Appendable): Unit = {
    "behave like a page with a submit button" in {
      val doc = asDocument(view())
      assertRenderedById(doc, "submit")
    }
  }

  //scalastyle:off method.length
  def pageWithDateFields(view: Form[_] => HtmlFormat.Appendable,
                         form: Form[_], idkey: String = "date",
                         msgKey: String = "date"): Unit = {

    val day = LocalDate.now().getDayOfMonth
    val year = LocalDate.now().getYear
    val month = LocalDate.now().getMonthValue
    val error = "error"

    val validData: Map[String, String] = Map(
      s"${idkey}.day" -> s"$day",
      s"${idkey}.month" -> s"$month",
      s"${idkey}.year" -> s"$year"
    )

    "display an input text box with the correct label and value for day" in {

      val v = view(form.bind(validData))

      val doc = asDocument(v)
      doc must haveLabelAndValue(s"${idkey}_day", messages("date_day"), s"$day")
    }

    "display an input text box with the correct label and value for month" in {
      val doc = asDocument(view(form.bind(validData)))
      doc must haveLabelAndValue(s"${idkey}_month", messages("date_month"), s"$month")
    }

    "display an input text box with the correct label and value for year" in {
      val doc = asDocument(view(form.bind(validData)))
      doc must haveLabelAndValue(s"${idkey}_year", messages("date_year"), s"$year")
    }

    "display error for day field on error summary" in {
      val error = "error"
      val doc = asDocument(view(form.withError(FormError(s"${idkey}.day", error))))
      doc must haveErrorOnSummary(s"${idkey}_day", error)
    }

    "display error for month field on error summary" in {
      val error = "error"
      val doc = asDocument(view(form.withError(FormError(s"${idkey}.month", error))))
      doc must haveErrorOnSummary(s"${idkey}_month", error)
    }

    "display error for year field on error summary" in {
      val error = "error"
      val doc = asDocument(view(form.withError(FormError(s"${idkey}.year", error))))
      doc must haveErrorOnSummary(s"${idkey}_year", error)
    }

    "display only one date error when all the date fields are missing" in {
      val expectedError = messages(s"messages__${msgKey}_error__all_blank")
      val invalidData: Map[String, String] = Map(
        "firstName" -> "testFirstName",
        "lastName" -> "testLastName"
      )
      val doc = asDocument(view(form.withError(FormError(s"${idkey}", expectedError))))
      doc must haveErrorOnSummary(s"${idkey}", expectedError)
    }

  }

  //scalastyle:on method.length
}
