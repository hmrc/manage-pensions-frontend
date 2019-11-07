/*
 * Copyright 2019 HM Revenue & Customs
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

  val variationSchemeName = "a scheme"
  val variationDeleteDate = "a date"

  val messageKeyPrefix = "schemesOverview"
  val schemeName = "Test Scheme Name"
  val lastDate: String = LocalDate.now.toString
  val deleteDate: String = LocalDate.now.plusDays(frontendAppConfig.daysDataSaved).toString
  private val psaId = "A0000000"
  private val srn = "123"

  private val adminCard = CardViewModel(
    id = Some("administrator-card"),
    heading = Message("messages__schemeOverview__psa_heading"),
    subHeading = Some(Message("messages__schemeOverview__psa_id", psaId)),
    links = Seq(
      Link("psaLink", frontendAppConfig.registeredPsaDetailsUrl, Message("messages__schemeOverview__psa_change")),
    Link("invitations-received", controllers.invitations.routes.YourInvitationsController.onPageLoad().url,
      Message("messages__schemeOverview__psa_view_invitations")
    ),
    Link("deregister-link", controllers.deregister.routes.ConfirmStopBeingPsaController.onPageLoad().url,
      Message("messages__schemeOverview__psa_deregister"))
  ))


  private val schemeCardWithNoActiveChanges = CardViewModel(
    id = Some("scheme-card"),
    heading = Message("messages__schemeOverview__scheme_heading"),
    links = Seq(
      Link("view-schemes", ListSchemesController.onPageLoad().url, Message("messages__schemeOverview__scheme_view")),
      Link("register-new-scheme", controllers.routes.SchemesOverviewController.onClickCheckIfSchemeCanBeRegistered().url,
        Message("messages__schemeOverview__scheme_subscription"))
    )
  )

  private val schemeCardWithActiveChanges = CardViewModel(
    id = Some("scheme-card"),
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

  def createView: () => HtmlFormat.Appendable = () =>
    schemesOverview(frontendAppConfig, "John Doe", Seq(adminCard, schemeCardWithActiveChanges) )(fakeRequest, messages)

  def createFreshView: () => HtmlFormat.Appendable = () =>
    schemesOverview(frontendAppConfig, "John Doe", Seq(adminCard, schemeCardWithNoActiveChanges))(fakeRequest, messages)

  "SchemesOverview view when a scheme has been partially defined and which has no scheme variation" must {
    behave like normalPage(
      createView,
      messageKeyPrefix,
      messages(s"messages__${messageKeyPrefix}__heading"),
      "_manage__text",
      "_manage__link",
      "_continue__link"
    )

    "have a name" in {
      createView must haveLink(frontendAppConfig.registeredPsaDetailsUrl, "psaLink")
      createView must haveElementWithText("psaName", "John Doe")
    }

    "display psa id" in {
      createView must haveElementWithText("psaId", psaId)
    }


    "display a link to your invitations page if user has received invitations" in {
      createView must haveLink( controllers.invitations.routes.YourInvitationsController.onPageLoad().url, "invitations-received")
    }


    "have link to view all schemes" in {
      createView must haveLink(controllers.routes.ListSchemesController.onPageLoad().url, "view-schemes")
    }


    "display scheme name" in {
      Jsoup.parse(createView().toString()) must haveDynamicText(schemeName)
    }

    "have dynamic text with date of last update" in {
      Jsoup.parse(createView().toString()) must
        haveDynamicText(Message("messages__schemesOverview__continue__lastDate", lastDate))
    }

    "have dynamic text with date of data deletion" in {
      Jsoup.parse(createView().toString()) must
        haveDynamicText(Message("messages__schemesOverview__continue__deleteDate", deleteDate))
    }

    "have link for continue registration" in {
      createView must
        haveLink(controllers.routes.SchemesOverviewController.onClickCheckIfSchemeCanBeRegistered().url, "continue-registration")
    }

    "have dyanamic text for delete registration" in {
      Jsoup.parse(createView().toString()) must
        haveDynamicText(messages("messages__schemesOverview__delete__link", schemeName))
    }

    "have link for delete registration" in {
      Jsoup.parse(createView().toString()).select("a[id=delete-registration]") must
        haveLink(controllers.routes.DeleteSchemeController.onPageLoad().url)
    }

    "have no variations section" in {
      Jsoup.parse(createView().toString()) mustNot haveDynamicText(messages("messages__schemesOverview__change_details__title"))

    }

  }

  "SchemesOverview view when a scheme variation is in progress" must {

    def variationsView = createView

    "have dynamic text with scheme name as section title" in {
      Jsoup.parse(variationsView().toString()) must
        haveDynamicText(Message("messages__schemesOverview__change_details__title", variationSchemeName))
    }

    "have dynamic text with scheme name and date of data deletion as p1" in {
      Jsoup.parse(variationsView().toString()) must
        haveDynamicText(Message("messages__schemesOverview__change_details__p1", variationSchemeName, variationDeleteDate))
    }

    "have link for continue variation" in {
      Jsoup.parse(variationsView().toString()).select("a[id=continue-variation]") must
        haveLink(frontendAppConfig.viewSchemeDetailsUrl.format(srn))
    }

    "have static text as p2" in {
      Jsoup.parse(variationsView().toString()) must
        haveDynamicText(Message("messages__schemesOverview__change_details__p2"))
    }

    "have link for delete variation" in {
      Jsoup.parse(variationsView().toString()).select("a[id=delete-variation]") must
        haveLink(controllers.routes.DeleteSchemeChangesController.onPageLoad(srn).url)
    }

  }

  "SchemesOverview view when a scheme has not been defined" must {
    behave like normalPage(
      createFreshView,
      messageKeyPrefix,
      messages(s"messages__${messageKeyPrefix}__heading"),
      "_manage__text",
      "_manage__link",
      "_register__head",
      "_register__text",
      "_register__link"
    )

    "have link to view all schemes" in {
      Jsoup.parse(createFreshView().toString()).select("a[id=view-schemes]") must
        haveLink(controllers.routes.ListSchemesController.onPageLoad().url)
    }


    "have link for registration" in {
      Jsoup.parse(createFreshView().toString()).select("a[id=register-new-scheme]") must
        haveLink(controllers.routes.SchemesOverviewController.onClickCheckIfSchemeCanBeRegistered().url)
    }

  }
}
