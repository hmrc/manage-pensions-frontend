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

import models.Link
import org.jsoup.Jsoup
import play.twirl.api.{Html, HtmlFormat}
import viewmodels.Message
import views.behaviours.ViewBehaviours
import views.html.schemeDetails

class SchemeDetailsViewSpec extends ViewSpecBase with ViewBehaviours {
  private val pspLinks = Seq(
    Link("view-practitioners", controllers.psp.routes.ViewPractitionersController.onPageLoad().url, Message("messages__pspViewOrDeauthorise__link")),
    Link("authorise", controllers.invitations.psp.routes.WhatYouWillNeedController.onPageLoad().url, Message("messages__pspAuthorise__link")))


  private val messageKeyPrefix = "schemeDetails"
  private val schemeName = "Test Scheme Name"
  private val openedDate = "29 February 2017"
  private val srn = "P12345678"
  private val pstr: Option[String] = Some("87654321XX")
  private val schemeDetailsView = injector.instanceOf[schemeDetails]

  def createView(date: Option[String] = Some(openedDate),
                 isSchemeOpen: Boolean = true,
                 displayChangeLink: Boolean = false,
                 lockingPsa: Option[String] = None,
                 aftHtml: Html = Html(""),
                 paymetsAndChargesHtml: Html = Html("")
                ): () => HtmlFormat.Appendable = () =>
    schemeDetailsView(
      schemeName,
      pstr,
      date,
      srn,
      isSchemeOpen,
      displayChangeLink,
      lockingPsa,
      aftHtml,
      paymetsAndChargesHtml,
      pspLinks
    )(fakeRequest, messages)

  "SchemesDetails view" must {
    behave like normalPageWithoutBrowserTitle(
      createView(),
      messageKeyPrefix,
      schemeName,
      "_psa_list_head",
      "_view_details_link",
      "_return_link",
      "_invite_link",
      "_view_psa"
    )

    "display the pstr" in {
      Jsoup.parse(createView()().toString()) must
        haveDynamicText(messages("messages__psaSchemeDash__pstr", pstr.get))
    }

    "have link to view scheme details" in {
      Jsoup.parse(createView(displayChangeLink = true)().toString()).select("a[id=view-details]") must
        haveLink(s"http://localhost:8200/register-pension-scheme/scheme-details/$srn")
      haveDynamicText(messages("messages__psaSchemeDash__view_details_link"))
    }

    "have link to view or change scheme details" in {
      Jsoup.parse(createView()().toString()).select("a[id=view-details]") must
        haveLink(s"http://localhost:8200/register-pension-scheme/scheme-details/$srn")
      haveDynamicText(messages("messages__psaSchemeDash__view_change_details_link"))
    }

    "display the date on which scheme was opened" in {
      Jsoup.parse(createView()().toString()) must
        haveDynamicText(openedDate)
    }

    "not display the date on which scheme was opened if no date is returned from API" in {

      Jsoup.parse(createView(None)().toString()) mustNot haveDynamicText(messages("messages__psaSchemeDash__opened_date_head"))
      Jsoup.parse(createView(None)().toString()) mustNot haveDynamicText(openedDate)
    }

    "contain section with links for administrators" in {
      Jsoup.parse(createView()().toString) must haveDynamicText("messages__psaSchemeDash__psa_list_head")
    }

    "have link to Invite another PSA" in {
      Jsoup.parse(createView()().toString()).select("a[id=invite]") must
        haveLink(controllers.invitations.routes.InviteController.onPageLoad(srn).url)
    }

    "render the 'View or remove' link if a PSA can be removed from the scheme" in {
      appRunning()
      createView() must haveLink(controllers.routes.ViewAdministratorsController.onPageLoad(srn).url, "view-psa-list")
    }


    "have link to authorise page" in {
      Jsoup.parse(createView()().toString()).select("a[id=authorise]") must
        haveLink(pspLinks.map(_.url).last)
    }

    "have link to view and deauthorise page" in {
      Jsoup.parse(createView()().toString()).select("a[id=view-practitioners]") must
        haveLink(pspLinks.map(_.url).head)
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
        haveDynamicText("messages__psaSchemeDash__psa_making_changes", "Gilderoy Lockhart")
    }

  }
}
