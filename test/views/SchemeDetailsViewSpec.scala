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
import play.twirl.api.HtmlFormat
import viewmodels.AssociatedPsa
import views.behaviours.ViewBehaviours
import views.html.schemeDetails

class SchemeDetailsViewSpec extends ViewSpecBase with ViewBehaviours {

  val messageKeyPrefix = "schemeDetails"
  val schemeName = "Test Scheme Name"
  val openedDate = "29 February 2017"
  val administrators = Seq(AssociatedPsa("First Psa", true), AssociatedPsa("Second User", false))
  val administratorsNoRemove = Seq(AssociatedPsa("First Psa", false), AssociatedPsa("Second User", false))
  val srn = "P12345678"

  class fakeFrontendAppConfig(invitationsEnabled: Boolean, workPackageTwoEnabled: Boolean) extends FrontendAppConfig(app.configuration, injector.instanceOf[Environment]) {
    override lazy val isWorkPackageOneEnabled: Boolean = invitationsEnabled
  }

    def createView(date: Option[String] = Some(openedDate),
                   psaList: Option[Seq[AssociatedPsa]] = Some(administrators),
                   invitations: Boolean = false,
                   workPackageTwoEnabled: Boolean = false,
                   isSchemeOpen: Boolean = true): () => HtmlFormat.Appendable = () =>
      schemeDetails(
        new fakeFrontendAppConfig(invitations, workPackageTwoEnabled),
        schemeName,
        date,
        psaList,
        srn,
        isSchemeOpen
      )(fakeRequest, messages)

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
          haveLink(s"http://localhost:8200/register-pension-scheme/scheme-details/$srn")
      }

      "display the date on which scheme was opened" in {
        Jsoup.parse(createView()().toString()) must
          haveDynamicText(openedDate)
      }

      "not display the date on which scheme was opened if no date is returned from API" in {

        Jsoup.parse(createView(None)().toString()) mustNot haveDynamicText(messages("messages__schemeDetails__opened_date_head"))
        Jsoup.parse(createView(None)().toString()) mustNot haveDynamicText(openedDate)
      }

      "contain list of administrators" in {
        for (psa <- administrators) Jsoup.parse(createView()().toString) must haveDynamicText(psa.name)
      }

      "render the 'Remove' link if a PSA can be removed from the scheme" in {
        createView(workPackageTwoEnabled = true) must haveLink(controllers.remove.routes.RemovePsaController.onPageLoad(srn).url, "remove-link")
      }

      "not render the 'Remove' link if no PSAs can be removed from the scheme" in {
        createView(psaList = Some(administratorsNoRemove), workPackageTwoEnabled = true) must notHaveElementWithId("remove-link")
      }

      "not contain list of administrators if not data is returned from API" in {
        Jsoup.parse(createView(psaList = None)().toString) mustNot haveDynamicText("messages__schemeDetails__psa_list_head")
        for (psa <- administrators)
          Jsoup.parse(createView(psaList = None)().toString) mustNot haveDynamicText(psa.name)
      }

      "have link to Invite another PSA" when {
        "invitations toggle is turned on" in {
          Jsoup.parse(createView(invitations = true)().toString()).select("a[id=invite]") must
            haveLink(controllers.invitations.routes.InviteController.onPageLoad(srn).url)
        }
      }

      "have the invite paragraph of content when invitations is toggled on" in {
        Jsoup.parse(createView(invitations = true)().toString()) must
          haveDynamicText("")
      }

      "not have link to Invite another PSA" when {
        "invitations toggle is turned off" in {
          Jsoup.parse(createView()().toString()).select("a[id=invite]") mustNot
            haveLink(controllers.routes.SchemeDetailsController.onPageLoad(srn).url)
        }

        "scheme status is not open" in {
          Jsoup.parse(createView(invitations = true, isSchemeOpen = false)().toString()).select("a[id=invite]") mustNot
            haveLink(controllers.routes.SchemeDetailsController.onPageLoad(srn).url)
        }
      }

      "have link to return to list of schemes page" in {
        Jsoup.parse(createView()().toString()).select("a[id=return]") must
          haveLink(controllers.routes.ListSchemesController.onPageLoad().url)
      }
    }
  }
