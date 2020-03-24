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

import base.SpecBase
import connectors.scheme.ListOfSchemesConnector
import models.ListOfSchemes
import models.SchemeDetail
import models.SchemeStatus
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class SchemeSearchServiceSpec extends SpecBase with MockitoSugar with ScalaFutures {

  import SchemeSearchServiceSpec._

  "Search" must {
    "return correct list of scheme details with search on correct pstr" in {
      val mockSchemesConnector = mock[ListOfSchemesConnector]
      when(mockSchemesConnector.getListOfSchemes(Matchers.eq(psaId))(any(), any()))
        .thenReturn(Future.successful(listOfSchemes))

      val schemeSearchService = new SchemeSearchService(mockSchemesConnector)
      val pstr = "24000001IN"

      whenReady(schemeSearchService.search(psaId, Some(pstr))) { result =>
        result mustBe fullSchemes.filter(_.pstr contains pstr)
      }
    }

    "return correct list of scheme details with search on correct srn" in {
      val mockSchemesConnector = mock[ListOfSchemesConnector]
      when(mockSchemesConnector.getListOfSchemes(Matchers.eq(psaId))(any(), any()))
        .thenReturn(Future.successful(listOfSchemes))

      val schemeSearchService = new SchemeSearchService(mockSchemesConnector)
      val srn = "S2400000005"

      whenReady(schemeSearchService.search(psaId, Some(srn))) { result =>
        result mustBe fullSchemes.filter(_.referenceNumber == srn)
      }
    }

    "return empty list for correct format pstr/srn but no match" in {
      val mockSchemesConnector = mock[ListOfSchemesConnector]
      val emptyList = ListOfSchemes("", "", None)
      when(mockSchemesConnector.getListOfSchemes(Matchers.eq(psaId))(any(), any()))
        .thenReturn(Future.successful(emptyList))

      val schemeSearchService = new SchemeSearchService(mockSchemesConnector)

      whenReady(schemeSearchService.search(psaId, Some("S2400000016"))) { result =>
        result mustBe Nil
      }
    }

    "return empty list for incorrect format pstr/srn" in {
      val mockSchemesConnector = mock[ListOfSchemesConnector]
      when(mockSchemesConnector.getListOfSchemes(Matchers.eq(psaId))(any(), any()))
        .thenReturn(Future.successful(listOfSchemes))

      val schemeSearchService = new SchemeSearchService(mockSchemesConnector)

      whenReady(schemeSearchService.search(psaId, Some("incorrectformat"))) { result =>
        result mustBe Nil
      }
    }
  }
}

object SchemeSearchServiceSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val psaId: String = "psaId"

  def listOfSchemes = ListOfSchemes("", "", Some(fullSchemes))

  def fullSchemes: List[SchemeDetail] =
    List(
      SchemeDetail(
        name = "scheme-1",
        referenceNumber = "srn-1",
        schemeStatus = SchemeStatus.Deregistered.value,
        openDate = None,
        pstr = Some("24000001IN"),
        relationShip = None,
        underAppeal = None
      ),
      SchemeDetail(
        name = "scheme-2",
        referenceNumber = "S2400000005",
        schemeStatus = SchemeStatus.Deregistered.value,
        openDate = None,
        pstr = Some("pstr-1"),
        relationShip = None,
        underAppeal = None
      )
    )
}
