package services

import org.scalatestplus.mockito.MockitoSugar
import base.SpecBase
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures

class SchemeSearchServiceSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach with ScalaFutures {
  //"onSearch" when {
  //
  //  when(mockMinimalPsaConnector.getPsaNameFromPsaID(any())(any(), any()))
  //    .thenReturn(Future.successful(Some(psaName)))
  //
  //  "return OK and the correct view when there are schemes without pagination and search on pstr" in {
  //    when(mockSchemeSearchService.search(any(),any())(any(),any())).thenReturn(Future.successful(fullSchemes))
  //    val pagination: Int = 10
  //
  //    val numberOfPages =
  //      paginationService.divide(fullSchemes.length, pagination)
  //
  //    when(mockAppConfig.listSchemePagination) thenReturn pagination
  //
  //    val matchPSTR = "24000001IN"
  //
  //    val fixture = testFixture(psaIdWithSchemes)
  //    val postRequest =
  //      fakeRequest.withFormUrlEncodedBody(("searchText", matchPSTR))
  //    val result = fixture.controller.onSearch(postRequest)
  //
  //    status(result) mustBe OK
  //
  //    val expectedSchemes = fullSchemes.filter(_.pstr.contains(matchPSTR))
  //
  //    val expected = viewAsString(
  //      schemes = expectedSchemes,
  //      numberOfSchemes = expectedSchemes.length,
  //      pagination = pagination,
  //      pageNumber = 1,
  //      pageNumberLinks = Seq.empty,
  //      numberOfPages = numberOfPages,
  //      noResultsMessageKey = None,
  //      Some(matchPSTR)
  //    )
  //
  //    contentAsString(result) mustBe expected
  //  }
  //
  //  "return OK and the correct view when there are schemes without pagination and search on srn" in {
  //    val pagination: Int = 10
  //
  //    val numberOfPages =
  //      paginationService.divide(fullSchemes.length, pagination)
  //
  //    when(mockAppConfig.listSchemePagination) thenReturn pagination
  //
  //    val matchSRN = "S2400000005"
  //
  //    val fixture = testFixture(psaIdWithSchemes)
  //    val postRequest =
  //      fakeRequest.withFormUrlEncodedBody(("searchText", matchSRN))
  //    val result = fixture.controller.onSearch(postRequest)
  //
  //    status(result) mustBe OK
  //
  //    val expectedSchemes = fullSchemes.filter(_.referenceNumber == matchSRN)
  //
  //    val expected = viewAsString(
  //      schemes = expectedSchemes,
  //      numberOfSchemes = expectedSchemes.length,
  //      pagination = pagination,
  //      pageNumber = 1,
  //      pageNumberLinks = Seq.empty,
  //      numberOfPages = numberOfPages,
  //      noResultsMessageKey = None,
  //      Some(matchSRN)
  //    )
  //
  //    contentAsString(result) mustBe expected
  //  }
  //

  //
  //  "return OK and the correct view when unrecognised format is entered into search" in {
  //    val pagination: Int = 10
  //
  //    val numberOfPages =
  //      paginationService.divide(fullSchemes.length, pagination)
  //
  //    when(mockAppConfig.listSchemePagination) thenReturn pagination
  //
  //    val incorrectSearchText = "Incorrect"
  //
  //    val fixture = testFixture(psaIdWithSchemes)
  //    val postRequest =
  //      fakeRequest.withFormUrlEncodedBody(("searchText", incorrectSearchText))
  //    val result = fixture.controller.onSearch(postRequest)
  //
  //    status(result) mustBe OK
  //
  //
  //    val expected = viewAsString(
  //      schemes = List.empty,
  //      numberOfSchemes = 0,
  //      pagination = pagination,
  //      pageNumber = 1,
  //      pageNumberLinks = Seq.empty,
  //      numberOfPages = numberOfPages,
  //      noResultsMessageKey = Some("messages__listSchemes__search_noMatches"),
  //      Some(incorrectSearchText)
  //    )
  //
  //    contentAsString(result) mustBe expected
  //  }
  //  "return OK and the correct view when correct format is entered into search but no results are found" in {
  //    val pagination: Int = 10
  //
  //    val numberOfPages =
  //      paginationService.divide(fullSchemes.length, pagination)
  //
  //    when(mockAppConfig.listSchemePagination) thenReturn pagination
  //
  //    val searchText = "S2400000016"
  //
  //    val fixture = testFixture(psaIdWithSchemes)
  //    val postRequest =
  //      fakeRequest.withFormUrlEncodedBody(("searchText", searchText))
  //    val result = fixture.controller.onSearch(postRequest)
  //
  //    status(result) mustBe OK
  //
  //
  //    val expected = viewAsString(
  //      schemes = List.empty,
  //      numberOfSchemes = 0,
  //      pagination = pagination,
  //      pageNumber = 1,
  //      pageNumberLinks = Seq.empty,
  //      numberOfPages = numberOfPages,
  //      noResultsMessageKey = Some("messages__listSchemes__search_noMatches"),
  //      Some(searchText)
  //    )
  //
  //    contentAsString(result) mustBe expected
  //  }
}
