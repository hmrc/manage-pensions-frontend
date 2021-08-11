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

import controllers.psa.routes._
import models.Link
import org.jsoup.Jsoup
import play.twirl.api.{Html, HtmlFormat}
import viewmodels.{CardSubHeading, CardSubHeadingParam, CardViewModel, Message}
import views.behaviours.ViewBehaviours
import views.html.schemesOverview

import java.time.LocalDate

class SchemesOverviewViewSpec extends ViewBehaviours {

  private val penaltiesTileHtml = Html("<p>test admin html</p>")
  private val migrationTileHtml = Html("<p>test migration html</p>")
  private val psaName = "John Doe"
  private val messageKeyPrefix = "schemeOverview"
  private val schemeName = "Test Scheme Name"
  private val lastDate: String = LocalDate.now.toString
  private val deleteDate: String = LocalDate.now.plusDays(frontendAppConfig.daysDataSaved).toString
  private val psaId = "A0000000"
  private val html: Html = Html("test-html")

  private val adminCard = CardViewModel(
    id = "administrator-card",
    heading = Message("messages__schemeOverview__psa_heading"),
    subHeadings = Seq(CardSubHeading(
      subHeading = Message("messages__schemeOverview__psa_id"),
      subHeadingClasses = "heading-small card-sub-heading",
      subHeadingParams = Seq(CardSubHeadingParam(
        subHeadingParam = psaId,
        subHeadingParamClasses = "font-small")))),
    links = Seq(
      Link("psaLink", frontendAppConfig.registeredPsaDetailsUrl, Message("messages__schemeOverview__psa_change")),
    Link("invitations-received", controllers.invitations.routes.YourInvitationsController.onPageLoad().url,
      Message("messages__schemeOverview__psa_view_invitations")
    ),
    Link("deregister-link", frontendAppConfig.psaDeregisterUrl,
      Message("messages__schemeOverview__psa_deregister"))
  ))


  private val schemeCard = CardViewModel(
    id = "scheme-card",
    heading = Message("messages__schemeOverview__scheme_heading"),
    links = Seq(
      Link("view-schemes", ListSchemesController.onPageLoad().url, Message("messages__schemeOverview__scheme_view"))
    ),
    html = Some(html)
  )

  private val schemesOverviewView = injector.instanceOf[schemesOverview]

  def createView: () => HtmlFormat.Appendable = () =>
    schemesOverviewView(psaName, "site.psa", Seq(adminCard, schemeCard), Some(penaltiesTileHtml), Some(migrationTileHtml))(fakeRequest, messages)

  def createFreshView: () => HtmlFormat.Appendable = () => schemesOverviewView(psaName,"site.psa",
    Seq(adminCard, schemeCard), Some(penaltiesTileHtml), Some(migrationTileHtml))(fakeRequest, messages)

  "SchemesOverview view when a scheme has been partially defined and which has no scheme variation" must {
    behave like normalPageWithoutBrowserTitle(
      createView,
      messageKeyPrefix,
      psaName,
      "_psa_heading",
      "_psa_id",
      "_psa_change",
      "_psa_view_invitations",
      "_psa_deregister",
      "_scheme_heading",
      "_scheme_view"
    )

    "have a name" in {
      createView must haveLink(frontendAppConfig.registeredPsaDetailsUrl, "psaLink")
    }

    "display a link to your invitations page if user has received invitations" in {
      createView must haveLink( controllers.invitations.routes.YourInvitationsController.onPageLoad().url, "invitations-received")
    }

    "display a link to stop being a psa" in {
      createView must haveLink(frontendAppConfig.psaDeregisterUrl, "deregister-link")
    }

    "have link to view all schemes" in {
      createView must haveLink(ListSchemesController.onPageLoad().url, "view-schemes")
    }

    "have html partial retrieved from scheme frontend" in {
      Jsoup.parse(createView().toString()) must
        haveDynamicText("test-html", schemeName)    }



  }
}
