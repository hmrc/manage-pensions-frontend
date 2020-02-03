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
import connectors.admin.MinimalPsaConnector
import connectors.aft.{AFTConnector, AftCacheConnector}
import connectors.scheme.PensionSchemeVarianceLockConnector
import identifiers.invitations.PSTRId
import models.{Link, SchemeVariance}
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.HeaderCarrier
import utils.UserAnswers
import viewmodels.{AFTViewModel, Message}

import scala.concurrent.Future

class SchemeDetailsServiceSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach with ScalaFutures {

  import SchemeDetailsServiceSpec._

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private val minimalPsaConnector: MinimalPsaConnector = mock[MinimalPsaConnector]
  private val lockConnector = mock[PensionSchemeVarianceLockConnector]
  private val aftConnector = mock[AFTConnector]

  private val aftCacheConnector = mock[AftCacheConnector]
//
//
//  override def beforeEach(): Unit = {
//    when(minimalPsaConnector.getPsaNameFromPsaID(eqTo(psaId))(any(), any()))
//      .thenReturn(Future.successful(minimalPsaName))
//
//    when(lockConnector.getLockByPsa(any())(any(), any()))
//      .thenReturn(Future.successful(Some(SchemeVariance(psaId, srn))))
//    super.beforeEach()
//  }




  def service: SchemeDetailsService =
    new SchemeDetailsService(frontendAppConfig, aftConnector, aftCacheConnector,
      lockConnector, minimalPsaConnector)

  "retrieveOptionAFTViewModel" must {
    "return the correct model when return is locked by another credentials" in {
      when(aftConnector.getListOfVersions(any())(any(), any()))
        .thenReturn(Future.successful(Some(Seq(1))))
      when(aftCacheConnector.lockedBy(any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(name)))
      val ua = UserAnswers().set(PSTRId)(pstr).asOpt.get

      whenReady(service.retrieveOptionAFTViewModel(ua, srn)) {
        _ mustBe lockedAftModel
      }
    }

    "return the correct model when return is not locked but versions is empty" in {
      when(aftConnector.getListOfVersions(any())(any(), any()))
        .thenReturn(Future.successful(Some(Nil)))
      when(aftCacheConnector.lockedBy(any(), any())(any(), any()))
        .thenReturn(Future.successful(None))
      val ua = UserAnswers().set(PSTRId)(pstr).asOpt.get

      whenReady(service.retrieveOptionAFTViewModel(ua, srn)) {
        _ mustBe unlockedEmptyAftModel
      }
    }

    "return the correct model when return is in progress but not locked" in {
      when(aftConnector.getListOfVersions(any())(any(), any()))
        .thenReturn(Future.successful(Some(Seq(1))))
      when(aftCacheConnector.lockedBy(any(), any())(any(), any()))
        .thenReturn(Future.successful(None))
      val ua = UserAnswers().set(PSTRId)(pstr).asOpt.get

      whenReady(service.retrieveOptionAFTViewModel(ua, srn)) {
        _ mustBe inProgressUnlockedAftModel
      }
    }
  }

//  "administratorVariations" must {
//    "return an associated psa when psa can remove themselves" in {
//      val ua = UserAnswers.
//    }
//  }

}

object SchemeDetailsServiceSpec {



  private val srn = "srn"
  private val pstr = "pstr"
  private val psaId = "A0000000"
  private val name = "test-name"
  val minimalPsaName = Some("John Doe Doe")


  val lockedAftModel = Some(
    AFTViewModel(
      Some(Message("messages__schemeDetails__aft_period")),
      Some(Message("messages__schemeDetails__aft_lockedBy", name)),
      Link(
        id = "aftSummaryPageLink",
        url = s"http://localhost:8206/manage-pension-scheme-accounting-for-tax/$srn/new-return/1/summary",
        linkText = Message("messages__schemeDetails__aft_view"))
    )
  )

  val unlockedEmptyAftModel = Some(
    AFTViewModel(
      None,
      None,
      Link(
        id = "aftChargeTypePageLink",
        url = s"http://localhost:8206/manage-pension-scheme-accounting-for-tax/$srn/new-return/charge-type",
        linkText = Message("messages__schemeDetails__aft_startLink"))
    )
  )

  val inProgressUnlockedAftModel = Option(
    AFTViewModel(
      Some(Message("messages__schemeDetails__aft_period")),
      Some(Message("messages__schemeDetails__aft_inProgress")),
      Link(
        id = "aftSummaryPageLink",
        url = s"http://localhost:8206/manage-pension-scheme-accounting-for-tax/$srn/new-return/1/summary",
        linkText = Message("messages__schemeDetails__aft_view"))
    )
  )
}