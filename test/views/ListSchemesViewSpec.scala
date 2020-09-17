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

import config.FrontendAppConfig
import controllers.ListSchemesControllerSpec.mockAppConfig
import controllers.actions.DataRetrievalAction
import forms.ListSchemesFormProvider
import models.SchemeDetails
import models.SchemeStatus
import org.jsoup.nodes.Document
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.guice.GuiceableModule
import play.api.mvc.Request
import play.twirl.api.HtmlFormat
import services.PaginationService
import views.behaviours.ViewBehaviours
import views.html.list_schemes
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.scalatest.Assertion
import org.scalatest.BeforeAndAfterEach

class ListSchemesViewSpec extends ViewSpecBase with ViewBehaviours with MockitoSugar with BeforeAndAfterEach{

  private val emptyList: List[SchemeDetails] = List.empty[SchemeDetails]
  private val pagination: Int = 10

  private val mockAppConfig = mock[FrontendAppConfig]

  override val injector = new GuiceApplicationBuilder()
    .overrides(
      Seq[GuiceableModule](
        bind[FrontendAppConfig].toInstance(mockAppConfig)
      ): _*
    ).injector()

  private val listSchemesView = injector.instanceOf[list_schemes]
  private val paginationService = new PaginationService
  private val listSchemesFormProvider = new ListSchemesFormProvider

  private val fullList: List[SchemeDetails] = List(
    SchemeDetails(
      "scheme-name-0",
      "reference-number-0",
      SchemeStatus.Pending.value,
      None,
      None,
      None,
      None
    ),
    SchemeDetails(
      "scheme-name-1",
      "reference-number-1",
      SchemeStatus.PendingInfoRequired.value,
      None,
      None,
      None,
      None
    ),
    SchemeDetails(
      "scheme-name-2",
      "reference-number-2",
      SchemeStatus.PendingInfoReceived.value,
      None,
      None,
      None,
      None
    ),
    SchemeDetails(
      "scheme-name-3",
      "reference-number-3",
      SchemeStatus.Rejected.value,
      None,
      None,
      None,
      None
    ),
    SchemeDetails(
      "scheme-name-4",
      "reference-number-4",
      SchemeStatus.Open.value,
      Option("2017-11-09"),
      Some("PSTR-4"),
      None,
      None
    ),
    SchemeDetails(
      "scheme-name-5",
      "reference-number-5",
      SchemeStatus.Deregistered.value,
      Option("2017-11-10"),
      Some("PSTR-5"),
      None,
      None
    ),
    SchemeDetails(
      "scheme-name-6",
      "reference-number-6",
      SchemeStatus.WoundUp.value,
      Option("2017-11-11"),
      Some("PSTR-6"),
      None,
      None
    ),
    SchemeDetails(
      "scheme-name-7",
      "reference-number-7",
      SchemeStatus.RejectedUnderAppeal.value,
      None,
      None,
      None,
      None
    )
  )

  private val psaName = "Test psa name"

  implicit private val request: Request[_] = fakeRequest

  override def beforeEach(): Unit = {
    when(mockAppConfig.minimumSchemeSearchResults) thenReturn 5
  }

  "list-schemes view" must {

    behave like normalPage(
      view = view(
        schemes = emptyList,
        numberOfSchemes = emptyList.length,
        pagination = pagination,
        pageNumber = 1,
        pageNumberLinks = Seq.empty,
        numberOfPages =
          paginationService.divide(numberOfSchemes = emptyList.length, pagination = pagination)
      ),
      messageKeyPrefix = "listSchemes",
      pageHeader = messages("messages__listSchemes__title")
    )

    "have link to redirect to Pension Schemes Online service" in {
      when(mockAppConfig.pensionSchemeOnlineServiceUrl) thenReturn "onlineserviceurl"
      view(schemes = emptyList,
        numberOfSchemes = emptyList.length,
        pagination = pagination,
        pageNumber = 1,
        pageNumberLinks = Seq.empty,
        numberOfPages =
          paginationService.divide(numberOfSchemes = emptyList.length, pagination = pagination)
      ) must haveLink(frontendAppConfig.pensionSchemeOnlineServiceUrl, "manage-link")
    }

    "have search bar when more than minimum schemes" in {

      val doc = asDocument(view(schemes = fullList,
        numberOfSchemes = emptyList.length,
        pagination = pagination,
        pageNumber = 1,
        pageNumberLinks = Seq.empty,
        numberOfPages =
          paginationService.divide(numberOfSchemes = emptyList.length, pagination = pagination)
      ).apply()
      )

      assertRenderedById(doc, "searchText-form")
    }

    "NOT have search bar when less than minimum schemes" in {
      when(mockAppConfig.minimumSchemeSearchResults) thenReturn 10
      val doc = asDocument(view(schemes = fullList,
        numberOfSchemes = emptyList.length,
        pagination = pagination,
        pageNumber = 1,
        pageNumberLinks = Seq.empty,
        numberOfPages =
          paginationService.divide(numberOfSchemes = emptyList.length, pagination = pagination)
      ).apply()
      )

      assertNotRenderedById(doc, "searchText-form")
    }

    "display a suitable message when there are no schemes to display" in {
      view(
        schemes = emptyList,
        numberOfSchemes = emptyList.length,
        pagination = pagination,
        pageNumber = 1,
        pageNumberLinks = Seq.empty,
        numberOfPages =
          paginationService.divide(numberOfSchemes = emptyList.length, pagination = pagination)
      ) must haveElementWithText("noSchemes", messages("messages__listSchemes__noSchemes"))
    }

    "display the correct column headers when there are schemes to display" in {
      val actual = view(
        schemes = fullList,
        numberOfSchemes = fullList.length,
        pagination = pagination,
        pageNumber = 1,
        pageNumberLinks = Seq.range(0, fullList.length),
        numberOfPages =
          paginationService.divide(numberOfSchemes = fullList.length, pagination = pagination)
      )

      actual must haveElementWithText("schemeNameHeader", messages("messages__listSchemes__column_schemeName"))
      actual must haveElementWithText("srnHeader", messages("messages__listSchemes__column_srn"))
      actual must haveElementWithText("pstrHeader", messages("messages__listSchemes__column_pstr"))
      actual must haveElementWithText("statusHeader", messages("messages__listSchemes__column_status"))
    }

    (0 to 7).foreach { index =>
      s"display the correct scheme name with links for row $index when there are schemes to display" in {
        val actual = asDocument(
          view(
            schemes = fullList,
            numberOfSchemes = fullList.length,
            pagination = pagination,
            pageNumber = 1,
            pageNumberLinks = Seq.range(0, fullList.length),
            numberOfPages =
              paginationService.divide(numberOfSchemes = fullList.length, pagination = pagination)
          ).apply()
        )

        actual must haveLinkWithUrlAndContent(s"schemeName-$index",
          controllers.routes.SchemeDetailsController.onPageLoad(s"reference-number-$index").url, s"scheme-name-$index")
      }
    }



    "display the full status value" in {
      val actual = asDocument(
        view(
          schemes = fullList,
          numberOfSchemes = fullList.length,
          pagination = pagination,
          pageNumber = 1,
          pageNumberLinks = Seq.range(0, fullList.length),
          numberOfPages =
            paginationService.divide(numberOfSchemes = fullList.length, pagination = pagination)
        ).apply()
      )

      assertEqualsValueOwnText(actual, "#schemeStatus-4", "Open")
      assertEqualsValueOwnText(actual, "#schemeStatus-5", "De-registered")
      assertEqualsValueOwnText(actual, "#schemeStatus-6", "Wound-up")

      assertEqualsValueOwnText(actual, "#schemeStatus-0", "Pending")
      assertEqualsValueOwnText(actual, "#schemeStatus-1", "Pending information required")
      assertEqualsValueOwnText(actual, "#schemeStatus-2", "Pending information received")
      assertEqualsValueOwnText(actual, "#schemeStatus-3", "Rejected")
      assertEqualsValueOwnText(actual, "#schemeStatus-7", "Rejected under appeal")
    }

    "show the SRN column with correct values" in {
      val actual = asDocument(
        view(
          schemes = fullList,
          numberOfSchemes = fullList.length,
          pagination = pagination,
          pageNumber = 1,
          pageNumberLinks = Seq.range(0, fullList.length),
          numberOfPages =
            paginationService.divide(numberOfSchemes = fullList.length, pagination = pagination)
        ).apply()
      )

      assertEqualsValueOwnText(actual, "#srn-0", messages("reference-number-0"))
      assertEqualsValueOwnText(actual, "#srn-1", messages("reference-number-1"))
      assertEqualsValueOwnText(actual, "#srn-2", messages("reference-number-2"))
      assertEqualsValueOwnText(actual, "#srn-3", messages("reference-number-3"))
      assertEqualsValueOwnText(actual, "#srn-4", messages("reference-number-4"))
      assertEqualsValueOwnText(actual, "#srn-5", messages("reference-number-5"))
      assertEqualsValueOwnText(actual, "#srn-6", messages("reference-number-6"))
      assertEqualsValueOwnText(actual, "#srn-7", messages("reference-number-7"))
    }

    "show the PSTR column with correct values" in {
      val actual = asDocument(
        view(
          schemes = fullList,
          numberOfSchemes = fullList.length,
          pagination = pagination,
          pageNumber = 1,
          pageNumberLinks = Seq.range(0, fullList.length),
          numberOfPages =
            paginationService.divide(numberOfSchemes = fullList.length, pagination = pagination)
        ).apply()
      )

      assertEqualsValueOwnText(actual, "#pstr-0", messages("messages__listSchemes__pstr_not_assigned"))
      assertEqualsValueOwnText(actual, "#pstr-1", messages("messages__listSchemes__pstr_not_assigned"))
      assertEqualsValueOwnText(actual, "#pstr-2", messages("messages__listSchemes__pstr_not_assigned"))
      assertEqualsValueOwnText(actual, "#pstr-3", messages("messages__listSchemes__pstr_not_assigned"))
      assertEqualsValueOwnText(actual, "#pstr-4", messages("PSTR-4"))
      assertEqualsValueOwnText(actual, "#pstr-5", messages("PSTR-5"))
      assertEqualsValueOwnText(actual, "#pstr-6", messages("PSTR-6"))
      assertEqualsValueOwnText(actual, "#pstr-7", messages("messages__listSchemes__pstr_not_assigned"))
    }

    "display a link to return to overview page" in {
      view(
        schemes = fullList,
        numberOfSchemes = fullList.length,
        pagination = pagination,
        pageNumber = 1,
        pageNumberLinks = Seq.range(0, fullList.length),
        numberOfPages =
          paginationService.divide(numberOfSchemes = fullList.length, pagination = pagination)
      ) must haveLink(controllers.routes.SchemesOverviewController.onPageLoad().url, "return-link")
    }

    "show correct pagination links when number of schemes is greater than pagination at start of range" in {
      val schemes: List[SchemeDetails] = List.fill(103)(fullList.head)

      val pageNumber: Int = 1

      val pagination: Int = 10

      val numberOfSchemes: Int = schemes.length

      val numberOfPages: Int = paginationService.divide(numberOfSchemes = numberOfSchemes, pagination = pagination)

      val pageNumberLinks: Seq[Int] = paginationService.pageNumberLinks(pageNumber, numberOfSchemes, pagination, numberOfPages)

      val actual = asDocument(
        view(
          schemes = schemes,
          numberOfSchemes = numberOfSchemes,
          pagination = pagination,
          pageNumber = pageNumber,
          pageNumberLinks = pageNumberLinks,
          numberOfPages = numberOfPages
        ).apply()
      )

      assertEqualsValue(actual, "#pagination-text", "Showing 1 to 10 of 103 schemes")
      assertNotRenderedByCssSelector(actual, "#first")
      assertNotRenderedByCssSelector(actual, "#prev")

      pageNumberLinks.foreach { index =>
        assertLink(actual, s"pageNumber-$index", controllers.routes.ListSchemesController.onPageLoadWithPageNumber(index).url)
      }

      assertNotRenderedByCssSelector(actual, "#pageNumber-6")
      assertLink(actual, "next", controllers.routes.ListSchemesController.onPageLoadWithPageNumber(2).url)
      assertLink(actual, "last", controllers.routes.ListSchemesController.onPageLoadWithPageNumber(numberOfPages).url)
      assertEqualsValue(actual, "#next", messages("messages__schemesOverview__pagination__next"))
      assertEqualsValue(actual, "#last", messages("messages__schemesOverview__pagination__last"))

    }

    "show correct pagination links when number of schemes is greater than pagination at middle of range" in {
      val schemes: List[SchemeDetails] = List.fill(103)(fullList.head)

      val pageNumber: Int = 4

      val pagination: Int = 10

      val numberOfSchemes: Int = schemes.length

      val numberOfPages: Int = paginationService.divide(numberOfSchemes = numberOfSchemes, pagination = pagination)

      val pageNumberLinks: Seq[Int] = paginationService.pageNumberLinks(pageNumber, numberOfSchemes, pagination, numberOfPages)

      val actual = asDocument(
        view(
          schemes = schemes,
          numberOfSchemes = numberOfSchemes,
          pagination = pagination,
          pageNumber = pageNumber,
          pageNumberLinks = pageNumberLinks,
          numberOfPages = numberOfPages
        ).apply()
      )

      assertEqualsValue(actual, "#pagination-text", "Showing 31 to 40 of 103 schemes")
      assertEqualsValue(actual, "#first", messages("messages__schemesOverview__pagination__first"))
      assertEqualsValue(actual, "#prev", messages("messages__schemesOverview__pagination__prev"))
      assertLink(actual, "first", controllers.routes.ListSchemesController.onPageLoadWithPageNumber(1).url)
      assertLink(actual, "prev", controllers.routes.ListSchemesController.onPageLoadWithPageNumber(3).url)

      pageNumberLinks.foreach { index =>
        assertLink(actual, s"pageNumber-$index", controllers.routes.ListSchemesController.onPageLoadWithPageNumber(index).url)
      }

      assertLink(actual, "next", controllers.routes.ListSchemesController.onPageLoadWithPageNumber(5).url)
      assertLink(actual, "last", controllers.routes.ListSchemesController.onPageLoadWithPageNumber(numberOfPages).url)
      assertEqualsValue(actual, "#next", messages("messages__schemesOverview__pagination__next"))
      assertEqualsValue(actual, "#last", messages("messages__schemesOverview__pagination__last"))
    }

    "show correct pagination links when number of schemes is greater than pagination at end of range" in {
      val schemes: List[SchemeDetails] = List.fill(103)(fullList.head)

      val pageNumber: Int = 11

      val pagination: Int = 10

      val numberOfSchemes: Int = schemes.length

      val numberOfPages: Int = paginationService.divide(numberOfSchemes = numberOfSchemes, pagination = pagination)

      val pageNumberLinks: Seq[Int] = paginationService.pageNumberLinks(pageNumber, numberOfSchemes, pagination, numberOfPages)

      val actual = asDocument(
        view(
          schemes = schemes,
          numberOfSchemes = numberOfSchemes,
          pagination = pagination,
          pageNumber = pageNumber,
          pageNumberLinks = pageNumberLinks,
          numberOfPages = numberOfPages
        ).apply()
      )

      assertEqualsValue(actual, "#pagination-text", "Showing 101 to 103 of 103 schemes")
      assertEqualsValue(actual, "#first", messages("messages__schemesOverview__pagination__first"))
      assertEqualsValue(actual, "#prev", messages("messages__schemesOverview__pagination__prev"))
      assertLink(actual, "first", controllers.routes.ListSchemesController.onPageLoadWithPageNumber(1).url)
      assertLink(actual, "prev", controllers.routes.ListSchemesController.onPageLoadWithPageNumber(10).url)

      pageNumberLinks.foreach { index =>
        assertLink(actual, s"pageNumber-$index", controllers.routes.ListSchemesController.onPageLoadWithPageNumber(index).url)
      }
      assertNotRenderedByCssSelector(actual, "#next")
      assertNotRenderedByCssSelector(actual, "#last")
    }

    "not show pagination links when number of schemes is less than pagination" in {
      val pagination: Int = 10

      val pageNumber: Int = 1

      val numberOfSchemes: Int = fullList.length

      val numberOfPages: Int = paginationService.divide(numberOfSchemes = numberOfSchemes, pagination = pagination)

      val pageNumberLinks: Seq[Int] = paginationService.pageNumberLinks(pageNumber, numberOfSchemes, pagination, numberOfPages)

      val actual = asDocument(
        view(
          schemes = fullList,
          numberOfSchemes = numberOfSchemes,
          pagination = pagination,
          pageNumber = pageNumber,
          pageNumberLinks = pageNumberLinks,
          numberOfPages = numberOfPages
        ).apply()
      )

      assertNotRenderedByCssSelector(actual, "#prev")
      assertNotRenderedByCssSelector(actual, "#next")
    }
  }

  private def view(schemes: List[SchemeDetails],
                   numberOfSchemes: Int,
                   pagination: Int,
                   pageNumber: Int,
                   pageNumberLinks: Seq[Int],
                   numberOfPages: Int
                  )(implicit request: Request[_], messages: Messages): () => HtmlFormat.Appendable = () =>
    listSchemesView(
      form = listSchemesFormProvider.apply(),
      schemes = schemes,
      psaName = psaName,
      numberOfSchemes = numberOfSchemes,
      pagination = pagination,
      pageNumber = pageNumber,
      pageNumberLinks = pageNumberLinks,
      numberOfPages = numberOfPages,
      noResultsMessageKey = Some("messages__listSchemes__noSchemes")
    )
}
