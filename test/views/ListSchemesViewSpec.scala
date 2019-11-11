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

import models.{SchemeDetail, SchemeStatus}
import play.api.i18n.Messages
import play.api.mvc.Request
import play.twirl.api.HtmlFormat
import views.behaviours.ViewBehaviours
import views.html.list_schemes

class ListSchemesViewSpec extends ViewSpecBase with ViewBehaviours {

  import ListSchemesViewSpec._

  implicit val request: Request[_] = fakeRequest

  "list-schemes view" must {

    behave like normalPage(view(), "listSchemes", messages("messages__listSchemes__title"))

    "have link to redirect to Pension Schemes Online service" in {
      view() must
        haveLink(frontendAppConfig.pensionSchemeOnlineServiceUrl, "manage-link")
    }

    "display a suitable message when there are no schemes to display" in {
      view() must haveElementWithText("noSchemes", messages("messages__listSchemes__noSchemes"))
    }

    "display the correct column headers when there are schemes to display" in {
      val actual = view(fullList)

      actual must haveElementWithText("schemeNameHeader", messages("messages__listSchemes__column_schemeName"))
      actual must haveElementWithText("srnHeader", messages("messages__listSchemes__column_srn"))
      actual must haveElementWithText("pstrHeader", messages("messages__listSchemes__column_pstr"))
      actual must haveElementWithText("statusHeader", messages("messages__listSchemes__column_status"))
    }

    (0 to 7).foreach { index =>
      s"display the correct scheme name with links for row $index when there are schemes to display" in {
        val actual = asDocument(view(fullList).apply())

        actual must haveLinkWithUrlAndContent(s"schemeName-$index",
          controllers.routes.SchemeDetailsController.onPageLoad(s"reference-number-$index").url, s"scheme-name-$index The scheme name is: scheme-name-$index")
      }
    }

    "display the full status value" in {
      val actual = asDocument(view(fullList).apply())

      assertEqualsValue(actual, "#schemeStatus-4 span:nth-child(1)", "Open")
      assertEqualsValue(actual, "#schemeStatus-5 span:nth-child(1)", "De-registered")
      assertEqualsValue(actual, "#schemeStatus-6 span:nth-child(1)", "Wound-up")

      assertEqualsValue(actual, "#schemeStatus-0 span:nth-child(1)", "Pending")
      assertEqualsValue(actual, "#schemeStatus-1 span:nth-child(1)", "Pending information required")
      assertEqualsValue(actual, "#schemeStatus-2 span:nth-child(1)", "Pending information received")
      assertEqualsValue(actual, "#schemeStatus-3 span:nth-child(1)", "Rejected")
      assertEqualsValue(actual, "#schemeStatus-7 span:nth-child(1)", "Rejected under appeal")
    }

    "show the SRN column with correct values" in {
      val actual = asDocument(view(fullList).apply())

      assertEqualsValue(actual, "#srn-0 span:nth-child(1)", messages("reference-number-0"))
      assertEqualsValue(actual, "#srn-1 span:nth-child(1)", messages("reference-number-1"))
      assertEqualsValue(actual, "#srn-2 span:nth-child(1)", messages("reference-number-2"))
      assertEqualsValue(actual, "#srn-3 span:nth-child(1)", messages("reference-number-3"))
      assertEqualsValue(actual, "#srn-4 span:nth-child(1)", messages("reference-number-4"))
      assertEqualsValue(actual, "#srn-5 span:nth-child(1)", messages("reference-number-5"))
      assertEqualsValue(actual, "#srn-6 span:nth-child(1)", messages("reference-number-6"))
      assertEqualsValue(actual, "#srn-7 span:nth-child(1)", messages("reference-number-7"))
    }

    "show the PSTR column with correct values" in {
      val actual = asDocument(view(fullList).apply())

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
      view() must haveLink(controllers.routes.SchemesOverviewController.onPageLoad().url, "return-link")
    }
  }
}

object ListSchemesViewSpec extends ViewSpecBase {
  val emptyList: List[SchemeDetail] = List.empty[SchemeDetail]

  val fullList: List[SchemeDetail] = List(
    SchemeDetail(
      "scheme-name-0",
      "reference-number-0",
      SchemeStatus.Pending.value,
      None,
      None,
      None,
      None
    ),
    SchemeDetail(
      "scheme-name-1",
      "reference-number-1",
      SchemeStatus.PendingInfoRequired.value,
      None,
      None,
      None,
      None
    ),
    SchemeDetail(
      "scheme-name-2",
      "reference-number-2",
      SchemeStatus.PendingInfoReceived.value,
      None,
      None,
      None,
      None
    ),
    SchemeDetail(
      "scheme-name-3",
      "reference-number-3",
      SchemeStatus.Rejected.value,
      None,
      None,
      None,
      None
    ),
    SchemeDetail(
      "scheme-name-4",
      "reference-number-4",
      SchemeStatus.Open.value,
      Option("2017-11-09"),
      Some("PSTR-4"),
      None,
      None
    ),
    SchemeDetail(
      "scheme-name-5",
      "reference-number-5",
      SchemeStatus.Deregistered.value,
      Option("2017-11-10"),
      Some("PSTR-5"),
      None,
      None
    ),
    SchemeDetail(
      "scheme-name-6",
      "reference-number-6",
      SchemeStatus.WoundUp.value,
      Option("2017-11-11"),
      Some("PSTR-6"),
      None,
      None
    ),
    SchemeDetail(
      "scheme-name-7",
      "reference-number-7",
      SchemeStatus.RejectedUnderAppeal.value,
      None,
      None,
      None,
      None
    )
  )
  
  val psaName = "Test psa name"
  
  def view(schemes: List[SchemeDetail] = emptyList)
          (implicit request: Request[_], messages: Messages): () => HtmlFormat.Appendable =
    () => list_schemes(frontendAppConfig, schemes, psaName)

  def viewAsString(schemes: List[SchemeDetail] = emptyList)
                  (implicit request: Request[_], messages: Messages): String = {
    val v = view(schemes)
    v().toString()
  }


}
