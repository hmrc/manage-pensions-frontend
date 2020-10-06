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

package views

import org.jsoup.Jsoup
import play.twirl.api.HtmlFormat
import views.behaviours.ViewBehaviours
import views.html.alreadyAssociatedWithScheme

class AlreadyAssociatedWithSchemeViewSpec extends ViewBehaviours {

  private val messageKeyPrefix = "alreadyAssociatedWithScheme"
  val schemeName  = "Test Scheme name"
  private val pspName = "PSP Name"
  private val alreadyAssociatedWithScheme = injector.instanceOf[alreadyAssociatedWithScheme]

  def createView: (() => HtmlFormat.Appendable) = () =>
    alreadyAssociatedWithScheme(pspName, schemeName)(fakeRequest, messages)

  "AlreadyAssociatedWithScheme page" must {
    behave like normalPageWithTitle(
      createView,
      messageKeyPrefix,
      messages(s"messages__${messageKeyPrefix}__title", pspName),
      messages(s"messages__${messageKeyPrefix}__heading", pspName)
    )

    "have the correct p1" in {
      assertContainsText(asDocument(createView()), messages(s"messages__${messageKeyPrefix}__p1", pspName, schemeName))
    }

    "have link to return to your scheme details" in {
      Jsoup.parse(createView().toString()).select("a[id=return-to-schemes]") must
        haveLink(controllers.routes.ListSchemesController.onPageLoad.url)
    }
  }
}
