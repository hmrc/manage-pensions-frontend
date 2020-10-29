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

package views.psp

import config.FrontendAppConfig
import forms.psp.ListSchemesFormProvider
import models.{SchemeDetails, SchemeStatus}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import play.api.mvc.Request
import play.twirl.api.HtmlFormat
import views.ViewSpecBase
import views.behaviours.ViewBehaviours
import views.html.psp.list_schemes

class ListSchemesViewSpec extends ViewSpecBase with ViewBehaviours with MockitoSugar with BeforeAndAfterEach {

  private val mockAppConfig = mock[FrontendAppConfig]
  override val injector = new GuiceApplicationBuilder()
    .overrides(
      Seq[GuiceableModule](
        bind[FrontendAppConfig].toInstance(mockAppConfig)
      ): _*
    ).injector()
  private val emptyList: List[SchemeDetails] = List.empty[SchemeDetails]
  private val listSchemesView = injector.instanceOf[list_schemes]
  private val listSchemesFormProvider = new ListSchemesFormProvider

//  private val fullList: List[SchemeDetails] = List(
//    SchemeDetails(
//      "scheme-name-0",
//      "reference-number-0",
//      SchemeStatus.Pending.value,
//      None,
//      None,
//      None,
//      None
//    ),
//    SchemeDetails(
//      "scheme-name-1",
//      "reference-number-1",
//      SchemeStatus.PendingInfoRequired.value,
//      None,
//      None,
//      None,
//      None
//    ),
//    SchemeDetails(
//      "scheme-name-2",
//      "reference-number-2",
//      SchemeStatus.PendingInfoReceived.value,
//      None,
//      None,
//      None,
//      None
//    ),
//    SchemeDetails(
//      "scheme-name-3",
//      "reference-number-3",
//      SchemeStatus.Rejected.value,
//      None,
//      None,
//      None,
//      None
//    ),
//    SchemeDetails(
//      "scheme-name-4",
//      "reference-number-4",
//      SchemeStatus.Open.value,
//      Option("2017-11-09"),
//      Some("PSTR-4"),
//      None,
//      None
//    ),
//    SchemeDetails(
//      "scheme-name-5",
//      "reference-number-5",
//      SchemeStatus.Deregistered.value,
//      Option("2017-11-10"),
//      Some("PSTR-5"),
//      None,
//      None
//    ),
//    SchemeDetails(
//      "scheme-name-6",
//      "reference-number-6",
//      SchemeStatus.WoundUp.value,
//      Option("2017-11-11"),
//      Some("PSTR-6"),
//      None,
//      None
//    ),
//    SchemeDetails(
//      "scheme-name-7",
//      "reference-number-7",
//      SchemeStatus.RejectedUnderAppeal.value,
//      None,
//      None,
//      None,
//      None
//    )
//  )

  private val psaName = "Test psa name"

  implicit private val request: Request[_] = fakeRequest

  override def beforeEach(): Unit = {
    when(mockAppConfig.minimumSchemeSearchResults) thenReturn 5
  }

  "list-schemes view" must {

    behave like normalPage(
      view = view(
        schemes = emptyList,
        numberOfSchemes = emptyList.length
      ),
      messageKeyPrefix = "listSchemes",
      pageHeader = messages("messages__listSchemesPsp__title")
    )

    "have link to redirect to Pension Schemes Online service" in {
      when(mockAppConfig.pensionSchemeOnlineServiceUrl) thenReturn "onlineserviceurl"
      view(schemes = emptyList,
        numberOfSchemes = emptyList.length
      ) must haveLink(frontendAppConfig.pensionSchemeOnlineServiceUrl, "manage-link")
    }
//
//    "have search bar when more than minimum schemes" in {
//
//      val doc = asDocument(view(schemes = fullList,
//        numberOfSchemes = emptyList.length
//      ).apply()
//      )
//
//      assertRenderedById(doc, "searchText-form")
//    }
//
//    "NOT have search bar when less than minimum schemes" in {
//      when(mockAppConfig.minimumSchemeSearchResults) thenReturn 10
//      val doc = asDocument(view(schemes = fullList,
//        numberOfSchemes = emptyList.length
//      ).apply()
//      )
//
//      assertNotRenderedById(doc, "searchText-form")
//    }
//
    "display a suitable message when there are no schemes to display" in {
      view(
       schemes = emptyList,
       numberOfSchemes = emptyList.length
     ) must haveElementWithText("noSchemes", messages("messages__listSchemesPsp__noMatchesLeft"))
    }
//
//    "display the correct column headers when there are schemes to display" in {
//      val actual = view(
//        schemes = fullList,
//        numberOfSchemes = fullList.length
//      )
//
//      actual must haveElementWithText("schemeNameHeader", messages("messages__listSchemesPsp__column__schemeName"))
//      actual must haveElementWithText("pstrHeader", messages("messages__listSchemesPsp__column__pstr"))
//      actual must haveElementWithText("statusHeader", messages("messages__listSchemesPsp__column__status"))
//    }
//
//  }
//
//
//  "display the full status value" in {
//    val actual = asDocument(
//      view(
//        schemes = fullList,
//        numberOfSchemes = fullList.length
//      ).apply()
//    )
//
//    assertEqualsValueOwnText(actual, "#schemeStatus-4", "Open")
//    assertEqualsValueOwnText(actual, "#schemeStatus-5", "De-registered")
//    assertEqualsValueOwnText(actual, "#schemeStatus-6", "Wound-up")
//
//    assertEqualsValueOwnText(actual, "#schemeStatus-0", "Pending")
//    assertEqualsValueOwnText(actual, "#schemeStatus-1", "Pending information required")
//    assertEqualsValueOwnText(actual, "#schemeStatus-2", "Pending information received")
//    assertEqualsValueOwnText(actual, "#schemeStatus-3", "Rejected")
//    assertEqualsValueOwnText(actual, "#schemeStatus-7", "Rejected under appeal")
//  }
//
//
//  "show the PSTR column with correct values" in {
//    val actual = asDocument(
//      view(
//        schemes = fullList,
//        numberOfSchemes = fullList.length
//      ).apply()
//    )
//
//    assertEqualsValueOwnText(actual, "#pstr-0", messages("messages__listSchemes__pstr_not_assigned"))
//    assertEqualsValueOwnText(actual, "#pstr-1", messages("messages__listSchemes__pstr_not_assigned"))
//    assertEqualsValueOwnText(actual, "#pstr-2", messages("messages__listSchemes__pstr_not_assigned"))
//    assertEqualsValueOwnText(actual, "#pstr-3", messages("messages__listSchemes__pstr_not_assigned"))
//    assertEqualsValueOwnText(actual, "#pstr-4", messages("PSTR-4"))
//    assertEqualsValueOwnText(actual, "#pstr-5", messages("PSTR-5"))
//    assertEqualsValueOwnText(actual, "#pstr-6", messages("PSTR-6"))
//    assertEqualsValueOwnText(actual, "#pstr-7", messages("messages__listSchemes__pstr_not_assigned"))
//  }
//
//  "display a link to return to overview page" in {
//    view(
//      schemes = fullList,
//      numberOfSchemes = fullList.length
//    ) must haveLink(controllers.routes.SchemesOverviewController.onPageLoad().url, "return-link")
  }


  private def view(schemes: List[SchemeDetails],
                   numberOfSchemes: Int
                  )(implicit request: Request[_], messages: Messages): () => HtmlFormat.Appendable = () =>

    listSchemesView(
      form = listSchemesFormProvider.apply(),
      schemes = schemes,
      psaName = psaName,
      numberOfSchemes = numberOfSchemes
    )
}
