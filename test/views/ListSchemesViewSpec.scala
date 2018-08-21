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
import play.api.mvc.Request
import play.twirl.api.HtmlFormat
import views.behaviours.ViewBehaviours
import views.html.list_schemes

class ListSchemesViewSpec extends ViewSpecBase with ViewBehaviours {

  import ListSchemesViewSpec._

  implicit val request: Request[_] = fakeRequest

  "list-schemes view" must {

    behave like normalPage(view(frontendAppConfig), "listSchemes", messages("messages__listSchemes__title"))

    behave like pageWithBackLink(view(frontendAppConfig))

    "display a suitable message when there are no schemes to display" in {
      view(frontendAppConfig) must haveElementWithText("noSchemes", messages("messages__listSchemes__noSchemes"))
    }

    "display a link to register a new scheme when there are no schemes to display" in {
      view(frontendAppConfig) must haveLink(frontendAppConfig.registerSchemeUrl, "registerNewScheme")
    }

    "display the correct column headers when there are schemes to display" in {
      val actual = view(frontendAppConfig, fullList)

      actual must haveElementWithText("schemeNameHeader1", messages("messages__listSchemes__column_schemeName"))
      actual must haveElementWithText("srnHeader2", messages("messages__listSchemes__column_srn"))
      actual must haveElementWithText("pstrHeader1", messages("messages__listSchemes__column_pstr"))
      actual must haveElementWithText("statusHeader1", messages("messages__listSchemes__column_status"))
    }

    "display the correct rows when there are schemes to display" in {
      val actual = asDocument(view(frontendAppConfig, fullList).apply())

      //table 1
      assertEqualsValue(actual, "#schemeName-1-0 span:nth-child(1)", "scheme-name-4")

      assertEqualsValue(actual, "#schemeName-1-1 span:nth-child(1)", "scheme-name-5")

      assertEqualsValue(actual, "#schemeName-1-2 span:nth-child(1)", "scheme-name-6")

      //table 2
      assertEqualsValue(actual, "#schemeName-2-0 span:nth-child(1)", "scheme-name-0")
      assertEqualsValue(actual, "#srn-2-0 span:nth-child(1)", "reference-number-0")

      assertEqualsValue(actual, "#schemeName-2-1 span:nth-child(1)", "scheme-name-1")
      assertEqualsValue(actual, "#srn-2-1 span:nth-child(1)", "reference-number-1")

      assertEqualsValue(actual, "#schemeName-2-2 span:nth-child(1)", "scheme-name-2")
      assertEqualsValue(actual, "#srn-2-2 span:nth-child(1)", "reference-number-2")

      assertEqualsValue(actual, "#schemeName-2-3 span:nth-child(1)", "scheme-name-3")
      assertEqualsValue(actual, "#srn-2-3 span:nth-child(1)", "reference-number-3")

      assertEqualsValue(actual, "#schemeName-2-4 span:nth-child(1)", "scheme-name-7")
      assertEqualsValue(actual, "#srn-2-4 span:nth-child(1)", "reference-number-7")
    }

    "display either PSTR or 'Not assigned' when there is no value" in {
      val actual = asDocument(view(frontendAppConfig, fullList).apply())

      //table 1
      assertEqualsValue(actual, "#pstr-1-0 span:nth-child(1)", "PSTR-4")
      assertEqualsValue(actual, "#pstr-1-1 span:nth-child(1)", "PSTR-5")
      assertEqualsValue(actual, "#pstr-1-2 span:nth-child(1)", "PSTR-6")
    }

    "display the full status value" in {
      val actual = asDocument(view(frontendAppConfig, fullList).apply())

      //table 1
      assertEqualsValue(actual, "#schemeStatus-1-0 span:nth-child(1)", "Open")
      assertEqualsValue(actual, "#schemeStatus-1-1 span:nth-child(1)", "De-registered")
      assertEqualsValue(actual, "#schemeStatus-1-2 span:nth-child(1)", "Wound-up")

      //table 2
      assertEqualsValue(actual, "#schemeStatus-2-0 span:nth-child(1)", "Pending")
      assertEqualsValue(actual, "#schemeStatus-2-1 span:nth-child(1)", "Pending information required")
      assertEqualsValue(actual, "#schemeStatus-2-2 span:nth-child(1)", "Pending information received")
      assertEqualsValue(actual, "#schemeStatus-2-3 span:nth-child(1)", "Rejected")
      assertEqualsValue(actual, "#schemeStatus-2-4 span:nth-child(1)", "Rejected under appeal")
    }

    "display the date" in {
      val actual = asDocument(view(frontendAppConfig, fullList).apply())

      assertEqualsValue(actual, "#schemeDate-1-0 span:nth-child(1)", "9 November 2017")
      assertEqualsValue(actual, "#schemeDate-1-1 span:nth-child(1)", "10 November 2017")
      assertEqualsValue(actual, "#schemeDate-1-2 span:nth-child(1)", "11 November 2017")
    }

    "display a scheme list with only schemes with a PSTR number" in {
      val actual = view(frontendAppConfig, fullList)
      val actualAsDoc = asDocument(actual.apply())

      actual must haveClassWithSize("row-group", 3, "schemeList-1")
      assertEqualsValue(actualAsDoc, "#schemeName-1-0 span:nth-child(1)", "scheme-name-4")
      assertEqualsValue(actualAsDoc, "#schemeName-1-1 span:nth-child(1)", "scheme-name-5")
      assertEqualsValue(actualAsDoc, "#schemeName-1-2 span:nth-child(1)", "scheme-name-6")
    }

    "display a scheme list with only schemes without a PSTR number" in {
      val actual = view(frontendAppConfig, fullList)
      val actualAsDoc = asDocument(actual.apply())

      actual must haveClassWithSize("row-group", 5, "schemeList-2")
      assertEqualsValue(actualAsDoc, "#schemeName-2-0 span:nth-child(1)", "scheme-name-0")
      assertEqualsValue(actualAsDoc, "#schemeName-2-1 span:nth-child(1)", "scheme-name-1")
      assertEqualsValue(actualAsDoc, "#schemeName-2-2 span:nth-child(1)", "scheme-name-2")
      assertEqualsValue(actualAsDoc, "#schemeName-2-3 span:nth-child(1)", "scheme-name-3")
      assertEqualsValue(actualAsDoc, "#schemeName-2-4 span:nth-child(1)", "scheme-name-7")
    }

    "show the PSTR table when there are schemes with PSTRs" in {
      val actual = asDocument(view(frontendAppConfig, PSTRSchemeList).apply())

      assertRenderedById(actual, "schemeList-1")
    }

    "not show the PSTR table when there are no schemes with PSTRs" in {
      val actual = asDocument(view(frontendAppConfig, noPSTRSchemeList).apply())

      assertNotRenderedById(actual, "schemeList-1")
    }

    "show the non-PSTR table when there are schemes without PSTRS" in {
      val actual = asDocument(view(frontendAppConfig, noPSTRSchemeList).apply())

      assertRenderedById(actual, "schemeList-2")
    }

    "not show the non-PSTR tables when there are no schemes without PSTRS" in {
      val actual = asDocument(view(frontendAppConfig, PSTRSchemeList).apply())

      assertNotRenderedById(actual, "schemeList-2")
    }

    "show the SRN column and header" when {
      "schemes have never been opened" in {
        val actual = asDocument(view(frontendAppConfig, noPSTRSchemeList).apply())

        assertRenderedById(actual, "srnHeader2")
        assertRenderedById(actual, "srn-2-0")
      }
    }

    "not show the SRN column and header" when {
      "schemes have been opened" in {
        val actual = asDocument(view(frontendAppConfig, PSTRSchemeList).apply())

        assertNotRenderedById(actual, "srn")
        assertNotRenderedById(actual, "srn-1-0")
      }
    }

    "show the PSTR column and header" when {
      "schemes have been opened" in {
        val actual = asDocument(view(frontendAppConfig, PSTRSchemeList).apply())

        assertRenderedById(actual, "pstrHeader1")
        assertRenderedById(actual, "pstr-1-0")
      }
    }

    "not show the PSTR column and header" when {
      "schemes have never been opened" in {
        val actual = asDocument(view(frontendAppConfig, noPSTRSchemeList).apply())

        assertNotRenderedById(actual, "pstr")
        assertNotRenderedById(actual, "pstr-1-0")
      }
    }

    "show the date column and header" when {
      "schemes have been opened" in {
        val actual = asDocument(view(frontendAppConfig, PSTRSchemeList).apply())

        assertRenderedById(actual, "schemeDateHeader1")
        assertRenderedById(actual, "schemeDate-1-0")
      }
    }

    "not show the date column and header" when {
      "schemes have never been opened" in {
        val actual = asDocument(view(frontendAppConfig, noPSTRSchemeList).apply())

        assertNotRenderedById(actual, "schemeDate")
        assertNotRenderedById(actual, "schemeDate-1-0")
      }
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

  val PSTRSchemeList: List[SchemeDetail] = List(
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
    )
  )

  val noPSTRSchemeList: List[SchemeDetail] = List(
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
      "scheme-name-7",
      "reference-number-7",
      "Rejected Under Appeal",
      None,
      None,
      None,
      None
    )
  )


  def view(appConfig: FrontendAppConfig, schemes: List[SchemeDetail] = emptyList)
          (implicit request: Request[_], messages: Messages): () => HtmlFormat.Appendable =
    () => list_schemes(appConfig, schemes)

  def viewAsString(appConfig: FrontendAppConfig, schemes: List[SchemeDetail] = emptyList)
                  (implicit request: Request[_], messages: Messages): String = {
    val v = view(appConfig, schemes)
    v().toString()
  }

}
