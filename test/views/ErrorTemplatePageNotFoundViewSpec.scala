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

package views

import org.jsoup.Jsoup
import play.twirl.api.HtmlFormat
import views.behaviours.ViewBehaviours
import views.html.error_template_page_not_found

class ErrorTemplatePageNotFoundViewSpec extends ViewBehaviours {

  private val messageKeyPrefix = "pageNotFound404"

  private val view = injector.instanceOf[error_template_page_not_found]

  def createView: () => HtmlFormat.Appendable = () =>
    view()(fakeRequest, messages)

  "Error template page not found page" must {
    behave like normalPageWithoutBrowserTitle(
      createView,
      messageKeyPrefix,
      messages(s"messages__${messageKeyPrefix}__heading"),
      "_p1",
      "_p2"
    )

    "display the correct browser title" in {
      val doc = asDocument(createView())
      assertEqualsMessage(doc, "title", messages(s"messages__${messageKeyPrefix}__title"))
    }

    "have link to return to your pension schemes" in {
      Jsoup.parse(createView().toString()).select("a[id=view-pension-schemes]") must
        haveLink(controllers.routes.ListSchemesController.onPageLoad().url)
    }
  }
}
