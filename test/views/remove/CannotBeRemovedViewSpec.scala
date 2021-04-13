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

import org.jsoup.Jsoup
import play.twirl.api.HtmlFormat
import viewmodels.RemovalViewModel
import views.behaviours.ViewBehaviours
import views.html.remove.psa.cannot_be_removed

class CannotBeRemovedViewSpec extends ViewBehaviours {

  private val messageKeyPrefix = "you_cannot_be_removed"
  private val view = injector.instanceOf[cannot_be_removed]

  private val viewModel : RemovalViewModel = RemovalViewModel(
    "messages__you_cannot_be_removed__title",
    "messages__you_cannot_be_removed__heading",
    "messages__you_cannot_be_removed__p1",
    "messages__you_cannot_be_removed__p2",
    "messages__you_cannot_be_removed__returnToSchemes__link")

  def createView: () => HtmlFormat.Appendable = () => view(viewModel)(fakeRequest, messages)

  "you cannot be removed page" must {
    behave like normalPage(
      createView,
      messageKeyPrefix,
      messages(s"messages__${messageKeyPrefix}__heading"),
      "_p1",
      "_p2"
    )

    "have link to return to your pension schemes" in {
      Jsoup.parse(createView().toString()).select("a[id=return-to-schemes]") must
        haveLink(controllers.psa.routes.ListSchemesController.onPageLoad().url)
    }
  }
}
