/*
 * Copyright 2018 HM Revenue & Customs
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

import config.FrontendAppConfig
import org.jsoup.Jsoup
import play.api.Environment
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder
import play.twirl.api.HtmlFormat
import views.behaviours.ViewBehaviours
import views.html.schemeDetails

class SchemeDetailsViewSpec extends ViewSpecBase with ViewBehaviours {

    val messageKeyPrefix = "schemeDetails"
    val schemeName = "Test Scheme Name"
    val openedDate = "29 February 2017"
    val administrators = Seq("First Psa", "Second User")

//    override lazy val app = new GuiceApplicationBuilder().configure(
//      "features.invitations" -> true
//    ).build()

  class fakeFrontendAppConfig(invitationsEnabled: Boolean) extends FrontendAppConfig(app.configuration, injector.instanceOf[Environment]) {
    override lazy val psaInvitationEnabled = invitationsEnabled
  }

    def createView(invitations: Boolean = false): () => HtmlFormat.Appendable = () =>
      schemeDetails(new fakeFrontendAppConfig(invitations), schemeName, openedDate, administrators)(fakeRequest, messages)

    "SchemesDetails view" must {
      behave like normalPage(
        createView(),
        messageKeyPrefix,
        schemeName,
        "_opened_date_head",
        "_psa_list_head",
        "_view_details_link",
        "_return_link"
      )

      "have link to view scheme details" in {
        Jsoup.parse(createView()().toString()).select("a[id=view-details]") must
          haveLink(controllers.routes.SchemeDetailsController.onPageLoad(0).url)
      }

      "display the date on which scheme was opened" in {
        Jsoup.parse(createView()().toString()) must
          haveDynamicText(openedDate)
      }

      "contain list of administrators" in {
        for (psa <- administrators)
          Jsoup.parse(createView()().toString) must haveDynamicText(psa)
      }

      "have link to Invite another PSA" when {
        "invitations toggle is turned on" in {
          Jsoup.parse(createView(invitations = true)().toString()).select("a[id=invite]") must
            haveLink(controllers.routes.SchemeDetailsController.onPageLoad(0).url)
        }
      }

      "not have link to Invite another PSA" when {
        "invitations toggle is turned off" in {
          Jsoup.parse(createView()().toString()).select("a[id=invite]") mustNot
            haveLink(controllers.routes.SchemeDetailsController.onPageLoad(0).url)
        }
      }

      "have link to return to list of schemes page" in {
        Jsoup.parse(createView()().toString()).select("a[id=return]") must
          haveLink(controllers.routes.ListSchemesController.onPageLoad.url)
      }
    }
  }
