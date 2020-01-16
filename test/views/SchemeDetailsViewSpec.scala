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

import models.Link
import org.jsoup.Jsoup
import play.twirl.api.HtmlFormat
import viewmodels.{AFTViewModel, AssociatedPsa, Message}
import views.behaviours.ViewBehaviours
import views.html.schemeDetails

class SchemeDetailsViewSpec extends ViewSpecBase with ViewBehaviours {

  val messageKeyPrefix = "schemeDetails"
  val schemeName = "Test Scheme Name"
  val openedDate = "29 February 2017"
  val administrators = Seq(AssociatedPsa("First Psa", true), AssociatedPsa("Second User", false))
  val administratorsNoRemove = Seq(AssociatedPsa("First Psa", false), AssociatedPsa("Second User", false))
  val srn = "P12345678"
  val pstr = Some("87654321XX")
  private val schemeDetailsView = injector.instanceOf[schemeDetails]

  def createView(date: Option[String] = Some(openedDate),
                 psaList: Option[Seq[AssociatedPsa]] = Some(administrators),
                 isSchemeOpen: Boolean = true,
                 displayChangeLink: Boolean = false,
                 lockingPsa: Option[String] = None,
                 optionAFTViewModel: Option[AFTViewModel] = None
                ): () => HtmlFormat.Appendable = () =>
    schemeDetailsView(
      schemeName,
      pstr,
      date,
      psaList,
      srn,
      isSchemeOpen,
      displayChangeLink,
      lockingPsa,
      optionAFTViewModel
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

    "display the pstr" in {
      Jsoup.parse(createView()().toString()) must
        haveDynamicText(messages("messages__schemeDetails__pstr", pstr.get))
    }

    "have link to view scheme details" in {
      Jsoup.parse(createView(displayChangeLink = true)().toString()).select("a[id=view-details]") must
        haveLink(s"http://localhost:8200/register-pension-scheme/scheme-details/$srn")
      haveDynamicText(messages("messages__schemeDetails__view_details_link"))
    }

    "have link to view or change scheme details" in {
      Jsoup.parse(createView()().toString()).select("a[id=view-details]") must
        haveLink(s"http://localhost:8200/register-pension-scheme/scheme-details/$srn")
      haveDynamicText(messages("messages__schemeDetails__view_change_details_link"))
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
      appRunning()
      createView() must haveLink(controllers.remove.routes.RemovePsaController.onPageLoad().url, "remove-link")
    }

    "not render the 'Remove' link if no PSAs can be removed from the scheme" in {
      createView(psaList = Some(administratorsNoRemove)) must notHaveElementWithId("remove-link")
    }

    "not contain list of administrators if not data is returned from API" in {
      Jsoup.parse(createView(psaList = None)().toString) mustNot haveDynamicText("messages__schemeDetails__psa_list_head")
      for (psa <- administrators)
        Jsoup.parse(createView(psaList = None)().toString) mustNot haveDynamicText(psa.name)
    }

    "have link to Invite another PSA" in {
      Jsoup.parse(createView()().toString()).select("a[id=invite]") must
        haveLink(controllers.invitations.routes.InviteController.onPageLoad(srn).url)

    }

    "have the invite paragraph of content" in {
      Jsoup.parse(createView()().toString()) must
        haveDynamicText("")
    }

    "not have link to Invite another PSA" when {
      "scheme status is not open" in {
        Jsoup.parse(createView(isSchemeOpen = false)().toString()).select("a[id=invite]") mustNot
          haveLink(controllers.routes.SchemeDetailsController.onPageLoad(srn).url)
      }
    }

    "have link to return to list of schemes page" in {
      Jsoup.parse(createView()().toString()).select("a[id=return]") must
        haveLink(controllers.routes.ListSchemesController.onPageLoad().url)
    }

    "render the name of PSA locking the same if applicable" in {
      Jsoup.parse(createView(lockingPsa = Some("Gilderoy Lockhart"))().toString) must
        haveDynamicText("messages__schemeDetails__psa_making_changes", "Gilderoy Lockhart")
    }

    "have a link for AFT when suitable model passed in" in {
      Jsoup.parse(createView(optionAFTViewModel = Some(AFTViewModel(None, None, Link("aftID", "url", "text"))))().toString()).select("a[id=aftID]") must
        haveLink("url")
    }

    "have a link for AFT and period and status when suitable model passed in" in {
      val result = Jsoup.parse(createView(optionAFTViewModel = Some(AFTViewModel(Some(Message("period")),
        Some(Message("status")),
        Link("aftID", "url", "text"))))().toString())

      result.select("a[id=aftID]") must haveLink("url")
      result.select("div[id=aftPeriod]") text() mustBe "period"
      result.select("div[id=aftStatus]") text() mustBe "status"
    }
  }
}
