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

import forms.ListSchemesFormProvider
import models.{SchemeDetail, SchemeStatus}
import play.api.i18n.Messages
import play.api.mvc.Request
import play.twirl.api.HtmlFormat
import services.PaginationService
import views.behaviours.ViewBehaviours
import views.html.list_schemes

class ListSchemesViewSpec extends ViewSpecBase with ViewBehaviours {

  private val emptyList: List[SchemeDetail] = List.empty[SchemeDetail]
  private val pagination: Int = 10
  private val listSchemesView = injector.instanceOf[list_schemes]
  private val paginationService = new PaginationService
  private val listSchemesFormProvider = new ListSchemesFormProvider

  private val fullList: List[SchemeDetail] = List(
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

  private val psaName = "Test psa name"

  implicit private val request: Request[_] = fakeRequest

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
      view(schemes = emptyList,
        numberOfSchemes = emptyList.length,
        pagination = pagination,
        pageNumber = 1,
        pageNumberLinks = Seq.empty,
        numberOfPages =
          paginationService.divide(numberOfSchemes = emptyList.length, pagination = pagination)
      ) must haveLink(frontendAppConfig.pensionSchemeOnlineServiceUrl, "manage-link")
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
          controllers.routes.SchemeDetailsController.onPageLoad(s"reference-number-$index").url, s"scheme-name-$index The scheme name is: scheme-name-$index")
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
      val schemes: List[SchemeDetail] = List.fill(103)(fullList.head)

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
      val schemes: List[SchemeDetail] = List.fill(103)(fullList.head)

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
      val schemes: List[SchemeDetail] = List.fill(103)(fullList.head)

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

  private def view(schemes: List[SchemeDetail],
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
