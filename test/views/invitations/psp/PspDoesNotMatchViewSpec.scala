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

package views.invitations.psp

import models.SchemeReferenceNumber
import org.jsoup.Jsoup
import play.api.mvc.Call
import play.twirl.api.HtmlFormat
import viewmodels.Message
import views.behaviours.ViewBehaviours
import views.html.invitations.psp.pspDoesNotMatch

class PspDoesNotMatchViewSpec extends ViewBehaviours {
  private val testPspName: String = "Joe Bloggs"
  private val testSchemeName: String = "Test Scheme Ltd"
  private val returnCall: Call = controllers.routes.PsaSchemeDashboardController.onPageLoad(SchemeReferenceNumber("srn"))

  private val messageKeyPrefix = "pspDoesNotMatch"

  private val view = injector.instanceOf[pspDoesNotMatch]

  def createView(): () => HtmlFormat.Appendable = () =>
    view(testSchemeName, testPspName, returnCall)(fakeRequest, messages)

  "Confirmation Page" must {

    behave like normalPage(
      createView(),
      messageKeyPrefix,
      Message("messages__pspDoesNotMatch__title"),
      "_p1"
    )

    "render whatHappensNext header" in {
      Jsoup.parse(createView()().toString()) must haveDynamicText(Message("messages__pspDoesNotMatch__psp", testPspName))
    }

    "render p2" in {
      Jsoup.parse(createView()().toString()) must haveDynamicText(Message("messages__pspDoesNotMatch__p2", testPspName))
    }

  }
}
