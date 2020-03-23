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


}
