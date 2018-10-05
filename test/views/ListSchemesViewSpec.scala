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
import models.SchemeDetail
import play.api.i18n.Messages
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Request
import play.twirl.api.HtmlFormat
import views.behaviours.ViewBehaviours
import views.html.list_schemes

class ListSchemesViewSpec extends ViewSpecBase with ViewBehaviours {

  import ListSchemesViewSpec._

  implicit val request: Request[_] = fakeRequest

  private def config(toggle: Boolean = true): FrontendAppConfig = {
    val injector = new GuiceApplicationBuilder().configure(
      "features.work-package-one-enabled" -> toggle
    ).build().injector
    injector.instanceOf[FrontendAppConfig]
  }

  "list-schemes view" must {

    behave like normalPage(view(config()), "listSchemes", messages("messages__listSchemes__title"))

    "display a link to invitations page if user has received invitations" in {
      view(frontendAppConfig, invitationsReceived = true) must haveLink(controllers.routes.ListSchemesController.onPageLoad().url, "invitations-received")
    }

    "display a suitable message when there are no schemes to display" in {
      view(config()) must haveElementWithText("noSchemes", messages("messages__listSchemes__noSchemes"))
    }

    "display a link to register a new scheme when there are no schemes to display" in {
      view(config()) must haveLink(frontendAppConfig.registerSchemeUrl, "registerNewScheme")
    }

    "display the correct column headers when there are schemes to display" in {
      val actual = view(config(), fullList)

      actual must haveElementWithText("schemeNameHeader", messages("messages__listSchemes__column_schemeName"))
      actual must haveElementWithText("pstrHeader", messages("messages__listSchemes__column_pstr"))
      actual must haveElementWithText("statusHeader", messages("messages__listSchemes__column_status"))
    }

    (0 to 7).foreach { index =>
      s"display the correct scheme name with links for row $index when there are schemes to display" in {
        val actual = asDocument(view(config(), fullList).apply())

        actual must haveLinkWithUrlAndContent(s"schemeName-$index",
          controllers.routes.SchemeDetailsController.onPageLoad(s"reference-number-$index").url, s"scheme-name-$index")
      }
    }

    (0 to 7).foreach { index =>
      s"display the correct scheme name with row $index when there are schemes to display" in {
        val actual = asDocument(view(config(false), fullList).apply())

        assertEqualsValue(actual, s"#schemeName-$index span:nth-child(1)", s"scheme-name-$index")
      }
    }

    "display the full status value" in {
      val actual = asDocument(view(config(), fullList).apply())

      assertEqualsValue(actual, "#schemeStatus-4 span:nth-child(1)", "Open")
      assertEqualsValue(actual, "#schemeStatus-5 span:nth-child(1)", "De-registered")
      assertEqualsValue(actual, "#schemeStatus-6 span:nth-child(1)", "Wound-up")

      assertEqualsValue(actual, "#schemeStatus-0 span:nth-child(1)", "Pending")
      assertEqualsValue(actual, "#schemeStatus-1 span:nth-child(1)", "Pending information required")
      assertEqualsValue(actual, "#schemeStatus-2 span:nth-child(1)", "Pending information received")
      assertEqualsValue(actual, "#schemeStatus-3 span:nth-child(1)", "Rejected")
      assertEqualsValue(actual, "#schemeStatus-7 span:nth-child(1)", "Rejected under appeal")
    }

    "show the PSTR column with correct values" in {
      val actual = asDocument(view(config(), fullList).apply())

      assertEqualsValue(actual, "#pstr-0 span:nth-child(1)", messages("messages__listSchemes__pstr_not_assigned"))
      assertEqualsValue(actual, "#pstr-1 span:nth-child(1)", messages("messages__listSchemes__pstr_not_assigned"))
      assertEqualsValue(actual, "#pstr-2 span:nth-child(1)", messages("messages__listSchemes__pstr_not_assigned"))
      assertEqualsValue(actual, "#pstr-3 span:nth-child(1)", messages("messages__listSchemes__pstr_not_assigned"))
      assertEqualsValue(actual, "#pstr-4 span:nth-child(1)", messages("PSTR-4"))
      assertEqualsValue(actual, "#pstr-5 span:nth-child(1)", messages("PSTR-5"))
      assertEqualsValue(actual, "#pstr-6 span:nth-child(1)", messages("PSTR-6"))
      assertEqualsValue(actual, "#pstr-7 span:nth-child(1)", messages("messages__listSchemes__pstr_not_assigned"))
    }

    "display a link to return to overview page" in {
      view(frontendAppConfig) must haveLink(controllers.routes.SchemesOverviewController.onPageLoad().url, "return-to-overview")
    }
  }
}

object ListSchemesViewSpec {
  val emptyList: List[SchemeDetail] = List.empty[SchemeDetail]

  val fullList: List[SchemeDetail] = List(
    SchemeDetail(
      "scheme-name-0",
      "reference-number-0",
      "Pending",
      None,
      None,
      None,
      None
    ),
    SchemeDetail(
      "scheme-name-1",
      "reference-number-1",
      "Pending Info Required",
      None,
      None,
      None,
      None
    ),
    SchemeDetail(
      "scheme-name-2",
      "reference-number-2",
      "Pending Info Received",
      None,
      None,
      None,
      None
    ),
    SchemeDetail(
      "scheme-name-3",
      "reference-number-3",
      "Rejected",
      None,
      None,
      None,
      None
    ),
    SchemeDetail(
      "scheme-name-4",
      "reference-number-4",
      "Open",
      Option("2017-11-09"),
      Some("PSTR-4"),
      None,
      None
    ),
    SchemeDetail(
      "scheme-name-5",
      "reference-number-5",
      "Deregistered",
      Option("2017-11-10"),
      Some("PSTR-5"),
      None,
      None
    ),
    SchemeDetail(
      "scheme-name-6",
      "reference-number-6",
      "Wound-up",
      Option("2017-11-11"),
      Some("PSTR-6"),
      None,
      None
    ),
    SchemeDetail(
      "scheme-name-7",
      "reference-number-7",
      "Rejected Under Appeal",
      None,
      None,
      None,
      None
    )
  )

  def view(appConfig: FrontendAppConfig, schemes: List[SchemeDetail] = emptyList, invitationsReceived: Boolean = false)
          (implicit request: Request[_], messages: Messages): () => HtmlFormat.Appendable =
    () => list_schemes(appConfig, schemes, invitationsReceived)

  def viewAsString(appConfig: FrontendAppConfig, schemes: List[SchemeDetail] = emptyList)
                  (implicit request: Request[_], messages: Messages): String = {
    val v = view(appConfig, schemes)
    v().toString()
  }

}