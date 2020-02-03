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
import models.{Link, SchemeVariance}
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import utils.UserAnswers
import viewmodels.{AFTViewModel, Message}

import scala.concurrent.Future

class SchemeDetailsServiceSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach with ScalaFutures {


  private val minimalPsaConnector: MinimalPsaConnector = mock[MinimalPsaConnector]
  private val lockConnector = mock[PensionSchemeVarianceLockConnector]
  private val aftConnector = mock[AFTConnector]
  private val aftCacheConnector = mock[AftCacheConnector]
  private val srn = "srn"
  private val name = "srn"

  override def beforeEach(): Unit = {
    when(minimalPsaConnector.getPsaNameFromPsaID(eqTo(psaId))(any(), any()))
      .thenReturn(Future.successful(minimalPsaName))
    when(invitationsCacheConnector.getForInvitee(any())(any(), any()))
      .thenReturn(Future.successful(invitationList))
    when(dataCacheConnector.fetch(any())(any(), any())).thenReturn(Future.successful(Some(schemeNameJsonOption)))
    when(dataCacheConnector.lastUpdated(any())(any(), any()))
      .thenReturn(Future.successful(Some(JsNumber(BigDecimal(timestamp)))))

    when(lockConnector.getLockByPsa(any())(any(), any()))
      .thenReturn(Future.successful(Some(SchemeVariance(psaId, srn))))
    when(updateConnector.fetch(any())(any(), any()))
      .thenReturn(Future.successful(Some(schemeNameJsonOption)))
    when(updateConnector.lastUpdated(any())(any(), any()))
      .thenReturn(Future.successful(Some(JsNumber(BigDecimal(timestamp)))))
    super.beforeEach()
  }

  val lockedModel = Some(
    AFTViewModel(
      Some(Message("messages__schemeDetails__aft_period")),
        Some(Message("messages__schemeDetails__aft_lockedBy", name)),
      Link(
        id = "aftSummaryPageLink",
        url = frontendAppConfig.aftSummaryPageUrl.format(srn, "1"),
        linkText = Message("messages__schemeDetails__aft_view"))
    )
  )

  def service: SchemeDetailsService =
    new SchemeDetailsService(frontendAppConfig, aftConnector, aftCacheConnector,
      lockConnector, minimalPsaConnector)

  "retrieveOptionAFTViewModel" must {
    "return the correct model when return is locked by another credentials" in {
      val ua = UserAnswers()
      val result = service.retrieveOptionAFTViewModel(ua, srn)
      whenReady(service.retrieveOptionAFTViewModel(ua, srn)) {
        _ mustBe
      }
    }
  }

}
