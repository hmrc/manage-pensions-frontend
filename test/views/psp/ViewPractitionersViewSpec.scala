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

package views.psp

import models.SchemeReferenceNumber
import org.jsoup.Jsoup
import play.api.mvc.Call
import play.twirl.api.HtmlFormat
import viewmodels.AuthorisedPractitioner
import views.behaviours.ViewBehaviours
import views.html.psp.viewPractitioners

class ViewPractitionersViewSpec extends ViewBehaviours {

  private val messageKeyPrefix = "viewPractitioners"
  val schemeName  = "Test Scheme name"
  val schemeSrn  = "12345"
  val returnCall: Call  = controllers.routes.SchemeDetailsController.onPageLoad(SchemeReferenceNumber(schemeSrn))
  val practitioners = Seq(AuthorisedPractitioner("Joe Bloggs", "Ann Bloggs", "02-01-2020"))
  private val viewPractitionersView = injector.instanceOf[viewPractitioners]


  def createView: (() => HtmlFormat.Appendable) = () =>
    viewPractitionersView(schemeName, returnCall, practitioners)(fakeRequest, messages)

  "ViewPractitioners page" must {
    behave like normalPage(
      createView,
      messageKeyPrefix,
      messages(s"messages__${messageKeyPrefix}__title")
    )

    "have link to return to your scheme details" in {
      Jsoup.parse(createView().toString()).select("a[id=return-link]") must
        haveLink(returnCall.url)
    }

    "have link to authorise page" in {
      Jsoup.parse(createView().toString()).select("a[id=authorise]") must
        haveLink(controllers.invitations.psp.routes.WhatYouWillNeedController.onPageLoad().url)
    }

  }
}
