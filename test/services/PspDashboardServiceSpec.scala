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
import connectors.admin.MinimalConnector
import controllers.routes.ListSchemesController
import models._
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import viewmodels.{CardViewModel, Message}

import scala.concurrent.Future
import scala.language.postfixOps

class PspDashboardServiceSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach with ScalaFutures {

  import PspDashboardServiceSpec._

  private val minimalPsaConnector: MinimalConnector = mock[MinimalConnector]

  override def beforeEach(): Unit = {
    when(minimalPsaConnector.getPsaNameFromPsaID(eqTo(pspId))(any(), any()))
      .thenReturn(Future.successful(Some(pspName)))
    super.beforeEach()
  }

  def service: PspDashboardService = new PspDashboardService(frontendAppConfig, minimalPsaConnector)

  "getTiles" must {
    "return tiles with relevant links when all possible links are displayed" in {
        service.getTiles(pspId) mustBe tiles
      }
  }

}

object PspDashboardServiceSpec extends SpecBase with MockitoSugar  {

  val pspName: String = "John Doe"
  private val pspId = "00000000"

  def minimalPsaDetails(psaSuspended: Boolean): MinimalPSAPSP = MinimalPSAPSP("test@test.com", psaSuspended, Some("Org Name"), None)

  private val practitionerCard: CardViewModel =
    CardViewModel(
      id = "practitioner-card",
      heading = Message("messages__pspDashboard__details_heading"),
      subHeading = Some(Message("messages__pspDashboard__psp_id")),
      subHeadingParam = Some(pspId),
      links = Seq(
        Link("pspLink", frontendAppConfig.pspDetailsUrl, Message("messages__pspDashboard__psp_change")),
        Link("deregister-link", frontendAppConfig.pspDeregisterUrl, Message("messages__pspDashboard__psp_deregister"))
      )
    )

  private def schemeCard: CardViewModel =
    CardViewModel(
      id = "scheme-card",
      heading = Message("messages__pspDashboard__scheme_heading"),
      links = Seq(Link("search-schemes", ListSchemesController.onPageLoad().url, Message("messages__pspDashboard__search_scheme")))
    )

  private val tiles: Seq[CardViewModel] = Seq(schemeCard, practitionerCard)

}



