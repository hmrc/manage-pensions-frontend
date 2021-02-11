/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers

import config.FrontendAppConfig
import connectors.FakeUserAnswersCacheConnector
import connectors.admin.MinimalConnector
import controllers.actions.{AuthAction, FakeAuthAction}
import forms.ListSchemesFormProvider
import models.{SchemeDetails, MinimalPSAPSP, SchemeStatus}
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import services.{PaginationService, SchemeSearchService}
//import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import views.html.list_schemes

import scala.concurrent.Future

class ListSchemesControllerSpec extends ControllerSpecBase with MockitoSugar with BeforeAndAfterEach {

  import ListSchemesControllerSpec._

  override def beforeEach(): Unit = {
    when(mockAppConfig.minimumSchemeSearchResults) thenReturn 1
  }

  "onPageLoad" when {
    "return OK and the correct view when there are no schemes" in {
      when(mockMinimalPsaConnector.getMinimalPsaDetails(any())(any(), any())).thenReturn(Future.successful(minimalPSAPSP()))
      when(mockSchemeSearchService.search(any(), any())(any(), any())).thenReturn(Future.successful(Nil))
      val pagination: Int = 10

      val numberOfPages = paginationService.divide(emptySchemes.length, pagination)

      when(mockAppConfig.listSchemePagination) thenReturn pagination

      val fixture = testFixture(psaIdNoSchemes)

      val result = fixture.controller.onPageLoad(fakeRequest)

      status(result) mustBe OK

      contentAsString(result) mustBe viewAsString(
        schemes = emptySchemes,
        numberOfSchemes = emptySchemes.length,
        pagination = pagination,
        pageNumber = 1,
        pageNumberLinks = Seq.empty,
        numberOfPages = numberOfPages,
        noResultsMessageKey = Some("messages__listSchemes__noSchemes"),
        formValue = None
      )
    }

    "rlsFlag is true and deceasedFlag is false return redirect to update contact page" in {
      when(mockMinimalPsaConnector.getMinimalPsaDetails(any())(any(), any()))
        .thenReturn(Future.successful(minimalPSAPSP(rlsFlag = true)))
      when(mockSchemeSearchService.search(any(), any())(any(), any())).thenReturn(Future.successful(Nil))

      when(mockAppConfig.psaUpdateContactDetailsUrl).thenReturn(dummyUrl)

      val fixture = testFixture(psaIdNoSchemes)

      val result = fixture.controller.onPageLoad(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(dummyUrl)
    }

    "rlsFlag is true and deceasedFlag is true return redirect to contact hmrc page" in {
      when(mockMinimalPsaConnector.getMinimalPsaDetails(any())(any(), any()))
        .thenReturn(Future.successful(minimalPSAPSP(rlsFlag = true, deceasedFlag = true)))
      when(mockSchemeSearchService.search(any(), any())(any(), any())).thenReturn(Future.successful(Nil))

      val fixture = testFixture(psaIdNoSchemes)

      val result = fixture.controller.onPageLoad(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.ContactHMRCController.onPageLoad().url)
    }

    "return OK and the correct view when there are schemes without pagination" in {
      when(mockMinimalPsaConnector.getMinimalPsaDetails(any())(any(), any())).thenReturn(Future.successful(minimalPSAPSP()))
      when(mockSchemeSearchService.search(any(), any())(any(), any())).thenReturn(Future.successful(fullSchemes))
      val pagination: Int = 10

      val numberOfPages = paginationService.divide(fullSchemes.length, pagination)

      when(mockAppConfig.listSchemePagination) thenReturn pagination

      val fixture = testFixture(psaIdWithSchemes)

      val result = fixture.controller.onPageLoad(fakeRequest)

      status(result) mustBe OK

      contentAsString(result) mustBe viewAsString(
        schemes = fullSchemes,
        numberOfSchemes = fullSchemes.length,
        pagination = pagination,
        pageNumber = 1,
        pageNumberLinks = Seq.empty,
        numberOfPages = numberOfPages,
        noResultsMessageKey = None,
        formValue = None
      )
    }

    "return OK and the correct view when there are schemes with pagination" in {
      when(mockMinimalPsaConnector.getMinimalPsaDetails(any())(any(), any())).thenReturn(Future.successful(minimalPSAPSP()))
      when(mockSchemeSearchService.search(any(), any())(any(), any())).thenReturn(Future.successful(fullSchemes))
      val pageNumber: Int = 1

      val pagination: Int = 1

      val numberOfSchemes: Int = fullSchemes.length

      val numberOfPages = paginationService.divide(numberOfSchemes, pagination)

      when(mockAppConfig.listSchemePagination) thenReturn pagination

      val fixture = testFixture(psaIdWithSchemes)

      val result = fixture.controller.onPageLoad(fakeRequest)

      status(result) mustBe OK

      contentAsString(result) mustBe viewAsString(
        schemes = fullSchemes.take(pagination),
        numberOfSchemes = numberOfSchemes,
        pagination = pagination,
        pageNumber = 1,
        pageNumberLinks = paginationService.pageNumberLinks(pageNumber, numberOfSchemes, pagination, numberOfPages),
        numberOfPages = numberOfPages,
        noResultsMessageKey = None,
        formValue = None
      )
    }

    "return OK and the correct view when using page number" in {
      when(mockMinimalPsaConnector.getMinimalPsaDetails(any())(any(), any())).thenReturn(Future.successful(minimalPSAPSP()))
      when(mockSchemeSearchService.search(any(), any())(any(), any())).thenReturn(Future.successful(fullSchemes))
      val pageNumber: Int = 2

      val pagination: Int = 1

      val numberOfSchemes: Int = fullSchemes.length

      val numberOfPages = paginationService.divide(numberOfSchemes, pagination)

      when(mockAppConfig.listSchemePagination) thenReturn pagination

      val fixture: TestFixture = testFixture(psaIdWithSchemes)

      val result = fixture.controller.onPageLoadWithPageNumber(pageNumber = pageNumber)(fakeRequest)

      status(result) mustBe OK

      contentAsString(result) mustBe viewAsString(
        schemes = fullSchemes.slice((pageNumber * pagination) - pagination, pageNumber * pagination),
        numberOfSchemes = numberOfSchemes,
        pagination = pagination,
        pageNumber = pageNumber,
        pageNumberLinks = paginationService.pageNumberLinks(pageNumber, numberOfSchemes, pagination, numberOfPages),
        numberOfPages = numberOfPages,
        noResultsMessageKey = None,
        formValue = None
      )
    }
  }

  "onSearch" when {
    "return OK and the correct view when there are schemes without pagination and search on non empty string" in {
      when(mockMinimalPsaConnector.getMinimalPsaDetails(any())(any(), any())).thenReturn(Future.successful(minimalPSAPSP()))
      val searchText = "24000001IN"
      when(mockSchemeSearchService.search(any(), Matchers.eq(Some(searchText)))(any(), any())).thenReturn(Future.successful(fullSchemes))
      val pagination: Int = 10

      val numberOfPages =
        paginationService.divide(fullSchemes.length, pagination)

      when(mockAppConfig.listSchemePagination) thenReturn pagination

      val fixture = testFixture(psaIdWithSchemes)
      val postRequest = fakeRequest.withFormUrlEncodedBody(("searchText", searchText))
      val result = fixture.controller.onSearch(postRequest)

      status(result) mustBe OK

      val expected = viewAsString(
        schemes = fullSchemes,
        numberOfSchemes = fullSchemes.length,
        pagination = pagination,
        pageNumber = 1,
        pageNumberLinks = Seq.empty,
        numberOfPages = numberOfPages,
        noResultsMessageKey = None,
        Some(searchText)
      )

      contentAsString(result) mustBe expected
    }

    "return BADREQUEST and error when no value is entered into search" in {
      when(mockMinimalPsaConnector.getMinimalPsaDetails(any())(any(), any())).thenReturn(Future.successful(minimalPSAPSP()))
      when(mockSchemeSearchService.search(any(), Matchers.eq(None))(any(), any())).thenReturn(Future.successful(fullSchemes))

      val pagination: Int = 10

      val numberOfPages = paginationService.divide(fullSchemes.length, pagination)

      when(mockAppConfig.listSchemePagination) thenReturn pagination

      val fixture = testFixture(psaIdWithSchemes)
      val postRequest = fakeRequest.withFormUrlEncodedBody(("searchText", ""))
      val result = fixture.controller.onSearch(postRequest)

      status(result) mustBe BAD_REQUEST

      val expected = viewAsString(
        schemes = fullSchemes,
        numberOfSchemes = fullSchemes.length,
        pagination = pagination,
        pageNumber = 1,
        pageNumberLinks = Seq.empty,
        numberOfPages = numberOfPages,
        noResultsMessageKey = None,
        Some("")
      )

      contentAsString(result) mustBe expected
    }

    "return OK and the correct view with correct no matches message when unrecognised format is entered into search" in {
      when(mockMinimalPsaConnector.getMinimalPsaDetails(any())(any(), any())).thenReturn(Future.successful(minimalPSAPSP()))
      val incorrectSearchText = "24000001IN"
      when(mockSchemeSearchService.search(any(), Matchers.eq(Some(incorrectSearchText)))(any(), any())).thenReturn(Future.successful(Nil))

      val pagination: Int = 10

      val numberOfPages =
        paginationService.divide(fullSchemes.length, pagination)

      when(mockAppConfig.listSchemePagination) thenReturn pagination

      val fixture = testFixture(psaIdWithSchemes)
      val postRequest =
        fakeRequest.withFormUrlEncodedBody(("searchText", incorrectSearchText))
      val result = fixture.controller.onSearch(postRequest)

      status(result) mustBe OK

      val expected = viewAsString(
        schemes = List.empty,
        numberOfSchemes = 0,
        pagination = pagination,
        pageNumber = 1,
        pageNumberLinks = Seq.empty,
        numberOfPages = numberOfPages,
        noResultsMessageKey = Some("messages__listSchemes__search_noMatches"),
        Some(incorrectSearchText)
      )

      contentAsString(result) mustBe expected
    }
  }
}

trait TestFixture {
  def controller: ListSchemesController
}

object ListSchemesControllerSpec extends ControllerSpecBase with MockitoSugar {
  private val psaIdNoSchemes: String = "A0000001"
  private val psaIdWithSchemes: String = "A0000002"
  private val psaName: String = "Test Psa Name"
  private def minimalPSAPSP(rlsFlag: Boolean = false, deceasedFlag: Boolean = false) = MinimalPSAPSP(
    email = "",
    isPsaSuspended = false,
    organisationName = Some(psaName),
    individualDetails = None,
    rlsFlag = rlsFlag,
    deceasedFlag = deceasedFlag
  )
  private val emptySchemes: List[SchemeDetails] = List.empty[SchemeDetails]
  private val mockMinimalPsaConnector: MinimalConnector =
    mock[MinimalConnector]
  private val mockAppConfig: FrontendAppConfig = mock[FrontendAppConfig]
  private val paginationService = new PaginationService
  private val listSchemesFormProvider = new ListSchemesFormProvider
  private val mockSchemeSearchService = mock[SchemeSearchService]

  private val fullSchemes: List[SchemeDetails] =
    List(
      SchemeDetails(
        name = "scheme-0",
        referenceNumber = "srn-0",
        schemeStatus = SchemeStatus.Open.value,
        openDate = None,
        pstr = Some("pstr-0"),
        relationship = None,
        underAppeal = None
      ),
      SchemeDetails(
        name = "scheme-1",
        referenceNumber = "srn-1",
        schemeStatus = SchemeStatus.Deregistered.value,
        openDate = None,
        pstr = Some("24000001IN"),
        relationship = None,
        underAppeal = None
      ),
      SchemeDetails(
        name = "scheme-2",
        referenceNumber = "S2400000005",
        schemeStatus = SchemeStatus.Deregistered.value,
        openDate = None,
        pstr = Some("pstr-2"),
        relationship = None,
        underAppeal = None
      ),
      SchemeDetails(
        name = "scheme-3",
        referenceNumber = "srn-3",
        schemeStatus = SchemeStatus.Deregistered.value,
        openDate = None,
        pstr = Some("pstr-3"),
        relationship = None,
        underAppeal = None
      ),
      SchemeDetails(
        name = "scheme-4",
        referenceNumber = "srn-4",
        schemeStatus = SchemeStatus.Deregistered.value,
        openDate = None,
        pstr = Some("pstr-4"),
        relationship = None,
        underAppeal = None
      ),
      SchemeDetails(
        name = "scheme-5",
        referenceNumber = "srn-5",
        schemeStatus = SchemeStatus.Deregistered.value,
        openDate = None,
        pstr = Some("pstr-5"),
        relationship = None,
        underAppeal = None
      ),
      SchemeDetails(
        name = "scheme-6",
        referenceNumber = "srn-6",
        schemeStatus = SchemeStatus.Deregistered.value,
        openDate = None,
        pstr = Some("pstr-6"),
        relationship = None,
        underAppeal = None
      ),
      SchemeDetails(
        name = "scheme-7",
        referenceNumber = "srn-7",
        schemeStatus = SchemeStatus.Deregistered.value,
        openDate = None,
        pstr = Some("pstr-7"),
        relationship = None,
        underAppeal = None
      )
    )
  private val view: list_schemes = app.injector.instanceOf[list_schemes]

  private def testFixture(psaId: String): TestFixture =
    new TestFixture with MockitoSugar {

      private def authAction(psaId: String): AuthAction =
        FakeAuthAction.createWithPsaId(psaId)

      override val controller: ListSchemesController =
        new ListSchemesController(
          mockAppConfig,
          messagesApi,
          authAction(psaId),
          getDataWithPsaName(psaId),
          mockMinimalPsaConnector,
          FakeUserAnswersCacheConnector,
          controllerComponents,
          view,
          paginationService,
          listSchemesFormProvider,
          mockSchemeSearchService
        )
    }

  private def viewAsString(schemes: List[SchemeDetails],
                           numberOfSchemes: Int,
                           pagination: Int,
                           pageNumber: Int,
                           pageNumberLinks: Seq[Int],
                           numberOfPages: Int,
                           noResultsMessageKey: Option[String],
                           formValue: Option[String]): String = {

    view(
      form = formValue
        .fold(listSchemesFormProvider())(v => listSchemesFormProvider().bind(Map("searchText" -> v))),
      schemes = schemes,
      psaName = psaName,
      numberOfSchemes = numberOfSchemes,
      pagination = pagination,
      pageNumber = pageNumber,
      pageNumberLinks = pageNumberLinks,
      numberOfPages = numberOfPages,
      noResultsMessageKey = noResultsMessageKey
    )(fakeRequest, messages).toString()
  }

  private val dummyUrl = "dummyURL"
}

