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

import java.time.LocalDate

import controllers.routes.ListSchemesController
import models.Link
import org.jsoup.Jsoup
import play.twirl.api.HtmlFormat
import viewmodels.{CardViewModel, Message}
import views.behaviours.ViewBehaviours
import views.html.schemesOverview

class SchemesOverviewViewSpec extends ViewBehaviours {

  val variationSchemeName = "Scheme being changed"
  val variationDeleteDate = "deletionDate"
  val psaName = "John Doe"
  val messageKeyPrefix = "schemeOverview"
  val schemeName = "Test Scheme Name"
  val lastDate: String = LocalDate.now.toString
  val deleteDate: String = LocalDate.now.plusDays(frontendAppConfig.daysDataSaved).toString
  private val psaId = "A0000000"
  private val srn = "123"

  private val adminCard = CardViewModel(
    id = "administrator-card",
    heading = Message("messages__schemeOverview__psa_heading"),
    subHeading = Some(Message("messages__schemeOverview__psa_id")),
    subHeadingParam = Some(psaId),
    links = Seq(
      Link("psaLink", frontendAppConfig.registeredPsaDetailsUrl, Message("messages__schemeOverview__psa_change")),
    Link("invitations-received", controllers.invitations.routes.YourInvitationsController.onPageLoad().url,
      Message("messages__schemeOverview__psa_view_invitations")
    ),
    Link("deregister-link", controllers.deregister.routes.ConfirmStopBeingPsaController.onPageLoad().url,
      Message("messages__schemeOverview__psa_deregister"))
  ))


  private val schemeCardWithNoActiveChanges = CardViewModel(
    id = "scheme-card",
    heading = Message("messages__schemeOverview__scheme_heading"),
    links = Seq(
      Link("view-schemes", ListSchemesController.onPageLoad().url, Message("messages__schemeOverview__scheme_view")),
      Link("register-new-scheme", controllers.routes.SchemesOverviewController.onClickCheckIfSchemeCanBeRegistered().url,
        Message("messages__schemeOverview__scheme_subscription"))
    )
  )

  private val schemeCardWithActiveChanges = CardViewModel(
    id = "scheme-card",
    heading = Message("messages__schemeOverview__scheme_heading"),
    links = Seq(
      Link("view-schemes", ListSchemesController.onPageLoad().url, Message("messages__schemeOverview__scheme_view")),
      Link("continue-registration", controllers.routes.SchemesOverviewController.onClickCheckIfSchemeCanBeRegistered().url,
        Message("messages__schemeOverview__scheme_subscription_continue", schemeName, deleteDate)),
      Link("delete-registration", controllers.routes.DeleteSchemeController.onPageLoad().url,
        Message("messages__schemeOverview__scheme_subscription_delete", schemeName)),
      Link("continue-variation", frontendAppConfig.viewSchemeDetailsUrl.format(srn),
        Message("messages__schemeOverview__scheme_variations_continue", schemeName, deleteDate)),
      Link("delete-variation", controllers.routes.DeleteSchemeChangesController.onPageLoad(srn).url,
        Message("messages__schemeOverview__scheme_variations_delete", schemeName))
    )
  )
  private val schemesOverviewView = injector.instanceOf[schemesOverview]

  def createView: () => HtmlFormat.Appendable = () =>
    schemesOverviewView(psaName, Seq(adminCard, schemeCardWithActiveChanges))(fakeRequest, messages)

  def createFreshView: () => HtmlFormat.Appendable = () => schemesOverviewView(psaName, Seq(adminCard, schemeCardWithNoActiveChanges))(fakeRequest, messages)

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
      createView must haveLink(controllers.deregister.routes.ConfirmStopBeingPsaController.onPageLoad().url, "deregister-link")
    }

    "have link to view all schemes" in {
      createView must haveLink(controllers.routes.ListSchemesController.onPageLoad().url, "view-schemes")
    }

    "have dynamic text with date of data deletion and scheme name for subscription" in {
      Jsoup.parse(createView().toString()) must
        haveDynamicText(Message("messages__schemeOverview__scheme_subscription_continue", schemeName, deleteDate))
    }

    "have link for continue registration" in {
      createView must
        haveLink(controllers.routes.SchemesOverviewController.onClickCheckIfSchemeCanBeRegistered().url, "continue-registration")
    }

    "have dynamic text for delete registration" in {
      Jsoup.parse(createView().toString()) must
        haveDynamicText(messages("messages__schemeOverview__scheme_subscription_delete", schemeName))
    }

    "have link for delete registration" in {
      Jsoup.parse(createView().toString()).select("a[id=delete-registration]") must
        haveLink(controllers.routes.DeleteSchemeController.onPageLoad().url)
    }

    "have dynamic text with date of data deletion and scheme name for variations" in {
      Jsoup.parse(createView().toString()) must
        haveDynamicText(Message("messages__schemeOverview__scheme_variations_continue", schemeName, deleteDate))
    }

    "have link for continue variation" in {
      createView must
        haveLink(frontendAppConfig.viewSchemeDetailsUrl.format(srn), "continue-variation")
    }

    "have dynamic text for delete variation" in {
      Jsoup.parse(createView().toString()) must
        haveDynamicText(messages("messages__schemeOverview__scheme_variations_delete", schemeName))
    }

    "have link for delete variation" in {
      Jsoup.parse(createView().toString()).select("a[id=delete-variation]") must
        haveLink(controllers.routes.DeleteSchemeChangesController.onPageLoad(srn).url)
    }

  }

  "SchemesOverview view when a scheme has not been defined" must {

    "have link for registration" in {
      Jsoup.parse(createFreshView().toString()).select("a[id=register-new-scheme]") must
        haveLink(controllers.routes.SchemesOverviewController.onClickCheckIfSchemeCanBeRegistered().url)
    }

  }
}
