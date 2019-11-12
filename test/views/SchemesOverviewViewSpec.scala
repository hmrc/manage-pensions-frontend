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

import models.{RegistrationDetails, VariationDetails}
import org.jsoup.Jsoup
import play.twirl.api.HtmlFormat
import viewmodels.Message
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
  private val srnOpt = Some(srn)
  private val schemesOverviewView = injector.instanceOf[schemesOverview]

  def createView(variationDetails:Option[VariationDetails] = None): () => HtmlFormat.Appendable = () =>
    schemesOverviewView(Some(RegistrationDetails(schemeName,
        deleteDate, lastDate)),
      Some("John Doe"),
      psaId,
      variationDetails)(fakeRequest, messages)

  def createFreshView: () => HtmlFormat.Appendable = () => schemesOverviewView(None, None, psaId, None)(fakeRequest, messages)

  "SchemesOverview view when a scheme has been partially defined and which has no scheme variation" must {
    behave like normalPage(
      createView(),
      messageKeyPrefix,
      messages(s"messages__${messageKeyPrefix}__heading"),
      "_manage__text",
      "_manage__link",
      "_continue__link"
    )

    "have a name" in {
      createView() must haveLink(frontendAppConfig.registeredPsaDetailsUrl, "psaLink")
      createView() must haveElementWithText("psaName", "John Doe")
    }

    "display psa id" in {
      createView() must haveElementWithText("psaId", psaId)
    }

    "not display the name when there is no name" in {
      createFreshView().toString() must not include "John Doe"
    }

    "have link to view all schemes" in {
      Jsoup.parse(createView()().toString()).select("a[id=view-schemes]") must
        haveLink(controllers.routes.ListSchemesController.onPageLoad().url)
    }

    "have link to redirect to Pension Schemes Online service" in {
      Jsoup.parse(createView()().toString()).select("a[id=manage-link]") must
        haveLink(frontendAppConfig.pensionSchemeOnlineServiceUrl)
    }

    "display scheme name" in {
      Jsoup.parse(createView()().toString()) must haveDynamicText(schemeName)
    }

    "have dynamic text with date of last update" in {
      Jsoup.parse(createView()().toString()) must
        haveDynamicText(Message("messages__schemesOverview__continue__lastDate", lastDate))
    }

    "have dynamic text with date of data deletion" in {
      Jsoup.parse(createView()().toString()) must
        haveDynamicText(Message("messages__schemesOverview__continue__deleteDate", deleteDate))
    }

    "have link for continue registration" in {
      Jsoup.parse(createView()().toString()).select("a[id=continue-registration]") must
        haveLink(controllers.routes.SchemesOverviewController.onClickCheckIfSchemeCanBeRegistered().url)
    }

    "have dyanamic text for delete registration" in {
      Jsoup.parse(createView()().toString()) must
        haveDynamicText(messages("messages__schemesOverview__delete__link", schemeName))
    }

    "have link for delete registration" in {
      Jsoup.parse(createView()().toString()).select("a[id=delete-registration]") must
        haveLink(controllers.routes.DeleteSchemeController.onPageLoad().url)
    }

    "have no variations section" in {
      Jsoup.parse(createView()().toString()) mustNot haveDynamicText(messages("messages__schemesOverview__change_details__title"))

    }

  }

  "SchemesOverview view when a scheme variation is in progress" must {

    def variationsView = createView(
      variationDetails = Some(VariationDetails(variationSchemeName, variationDeleteDate, srn)))

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

    "have link to redirect to Pension Schemes Online service" in {
      Jsoup.parse(createFreshView().toString()).select("a[id=manage-link]") must
        haveLink(frontendAppConfig.pensionSchemeOnlineServiceUrl)
    }

    "have link for registration" in {
      Jsoup.parse(createFreshView().toString()).select("a[id=register-new-scheme]") must
        haveLink(controllers.routes.SchemesOverviewController.onClickCheckIfSchemeCanBeRegistered().url)
    }

  }
}
