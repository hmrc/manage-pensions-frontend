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

import java.time.LocalDate

import org.jsoup.Jsoup
import play.twirl.api.HtmlFormat
import viewmodels.Message
import views.behaviours.ViewBehaviours
import views.html.schemesOverview

class SchemesOverviewViewSpec extends ViewBehaviours {

  val messageKeyPrefix = "schemesOverview"
  val schemeName = "Test Scheme Name"
  val lastDate: String = LocalDate.now.toString
  val deleteDate: String = LocalDate.now.plusDays(frontendAppConfig.daysDataSaved).toString

  def createView: (() => HtmlFormat.Appendable) = () =>
    schemesOverview(frontendAppConfig, Some(schemeName), Some(lastDate), Some(deleteDate), Some("John Doe"))(fakeRequest, messages)

  def createFreshView: () => HtmlFormat.Appendable = () => schemesOverview(frontendAppConfig, None, None, None, None)(fakeRequest, messages)

  "SchemesOverview view when a scheme has been partially defined" must {
    behave like normalPage(
      createView,
      messageKeyPrefix,
      messages(s"messages__${messageKeyPrefix}__heading"),
      "_manage__head",
      "_manage__text",
      "_manage__link",
      "_continue__link",
      "_delete__link",
      "_UR__head",
      "_UR__text",
      "_UR__link"
    )

    "have a name" in {
      createView().toString() must include("John Doe")
    }

    "not display the name when there is no name" in {
      createFreshView().toString() must not include "John Doe"
    }

    "have link to view all schemes" in {
      Jsoup.parse(createView().toString()).select("a[id=view-schemes]") must
        haveLink(controllers.routes.ListSchemesController.onPageLoad.url)
    }

    "have link to redirect to Pension Schemes Online service" in {
      Jsoup.parse(createView().toString()).select("a[id=manage-link]") must
        haveLink(frontendAppConfig.pensionSchemeOnlineServiceUrl)
    }

    "display scheme name" in {
      Jsoup.parse(createView().toString()) must
        haveDynamicText(Message("messages__schemesOverview__scheme_name", schemeName))
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
      Jsoup.parse(createView().toString()).select("a[id=continue-registration]") must
        haveLink(controllers.routes.SchemesOverviewController.onClickCheckIfSchemeCanBeRegistered().url)
    }

    "have link for delete registration" in {
      Jsoup.parse(createView().toString()).select("a[id=delete-registration]") must
        haveLink(frontendAppConfig.deleteSchemeUrl)
    }

    "have link for user research participation" in {
      Jsoup.parse(createView().toString()).select("a[id=user-research]") must
        haveLink(frontendAppConfig.userResearchUrl)
    }

  }

  "SchemesOverview view when a scheme has not been defined" must {
    behave like normalPage(
      createFreshView,
      messageKeyPrefix,
      messages(s"messages__${messageKeyPrefix}__heading"),
      "_manage__head",
      "_manage__text",
      "_manage__link",
      "_register__head",
      "_register__text",
      "_register__link"
    )

    "have link to view all schemes" in {
      Jsoup.parse(createFreshView().toString()).select("a[id=view-schemes]") must
        haveLink(controllers.routes.ListSchemesController.onPageLoad.url)
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
