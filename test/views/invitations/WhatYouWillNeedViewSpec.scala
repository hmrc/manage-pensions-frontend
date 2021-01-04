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

import models.SchemeReferenceNumber
import org.jsoup.Jsoup
import play.api.mvc.Call
import play.twirl.api.HtmlFormat
import views.behaviours.ViewBehaviours
import views.html.invitations.whatYouWillNeed

class WhatYouWillNeedViewSpec extends ViewBehaviours {

  private val messageKeyPrefix = "whatYouWillNeed"
  val schemeName  = "Test Scheme name"
  val schemeSrn  = "12345"
  val returnCall: Call  = controllers.routes.SchemeDetailsController.onPageLoad(SchemeReferenceNumber(schemeSrn))
  private val whatYouWillNeedView = injector.instanceOf[whatYouWillNeed]

  def createView: (() => HtmlFormat.Appendable) = () =>
    whatYouWillNeedView(schemeName, returnCall)(fakeRequest, messages)

  "WhatYouWillNeed page" must {
    behave like normalPage(
      createView,
      messageKeyPrefix,
      messages(s"messages__${messageKeyPrefix}__heading"),
      "_lede",
      "_list_heading",
      "_item1",
      "_item2"
    )

    "have link to return to your scheme details" in {
      Jsoup.parse(createView().toString()).select("a[id=return-link]") must
        haveLink(returnCall.url)
    }
  }
}
