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
import models.{ListOfSchemes, SchemeDetail, SchemeStatus}
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.HeaderCarrier
import utils.FuzzyMatching

import scala.concurrent.Future

class SchemeSearchServiceSpec extends SpecBase with MockitoSugar with ScalaFutures {

  import SchemeSearchServiceSpec._

  private val mockSchemesConnector = mock[ListOfSchemesConnector]
  private val mockFuzzyMatching = mock[FuzzyMatching]
  private val pstr = "24000001IN"
  private val srn = "S2400000005"
  private val schemeSearchService = new SchemeSearchService(mockSchemesConnector, mockFuzzyMatching)

  "Search" must {
    "return correct list of scheme details with search on correct pstr" in {
      when(mockSchemesConnector.getListOfSchemes(Matchers.eq(psaId))(any(), any()))
        .thenReturn(Future.successful(listOfSchemes))

      whenReady(schemeSearchService.search(psaId, Some(pstr))) { result =>
        result mustBe fullSchemes.filter(_.pstr contains pstr)
      }
    }

    "return correct list of scheme details with search on correct srn" in {
      when(mockSchemesConnector.getListOfSchemes(Matchers.eq(psaId))(any(), any()))
        .thenReturn(Future.successful(listOfSchemes))

      whenReady(schemeSearchService.search(psaId, Some(srn))) { result =>
        result mustBe fullSchemes.filter(_.referenceNumber == srn)
      }
    }

    "return empty list for correct format pstr/srn but no match" in {
      val emptyList = ListOfSchemes("", "", None)
      when(mockSchemesConnector.getListOfSchemes(Matchers.eq(psaId))(any(), any()))
        .thenReturn(Future.successful(emptyList))

      whenReady(schemeSearchService.search(psaId, Some("S2400000016"))) { result =>
        result mustBe Nil
      }
    }

    "return correct list of scheme details with search on scheme name" in {
      when(mockFuzzyMatching.doFuzzyMatching(any(), any())).thenReturn(true).thenReturn(false)
      when(mockSchemesConnector.getListOfSchemes(Matchers.eq(psaId))(any(), any()))
        .thenReturn(Future.successful(listOfSchemes))

      whenReady(schemeSearchService.search(psaId, Some("scheme-1"))) { result =>
        result mustBe fullSchemes.filter(_.name == "scheme-1")
      }
    }

    "return empty list when fuzzy matching fails" in {
      when(mockFuzzyMatching.doFuzzyMatching(any(), any())).thenReturn(false)
      val emptyList = ListOfSchemes("", "", None)
      when(mockSchemesConnector.getListOfSchemes(Matchers.eq(psaId))(any(), any()))
        .thenReturn(Future.successful(emptyList))

      whenReady(schemeSearchService.search(psaId, Some("no matching"))) { result =>
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
