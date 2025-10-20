/*
 * Copyright 2024 HM Revenue & Customs
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
import config.FrontendAppConfig
import connectors.InvitationsCacheConnector
import connectors.admin.MinimalConnector
import connectors.scheme.ListOfSchemesConnector
import models.*
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import viewmodels.{CardSubHeading, CardSubHeadingParam, CardViewModel, Message}

import scala.concurrent.Future

class PspDashboardServiceSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach with ScalaFutures {

  import PspDashboardServiceSpec._

  private val minimalPsaConnector: MinimalConnector = mock[MinimalConnector]
  private val frontendAppConfig: FrontendAppConfig = mock[FrontendAppConfig]
  private val invitationsCacheConnector:InvitationsCacheConnector = mock[InvitationsCacheConnector]
  private val listOfSchemesConnector = mock[ListOfSchemesConnector]


  override lazy val app: Application = GuiceApplicationBuilder()
    .overrides(
      bind[MinimalConnector].toInstance(minimalPsaConnector),
      bind[FrontendAppConfig].toInstance(frontendAppConfig),
      bind[InvitationsCacheConnector].toInstance(invitationsCacheConnector),
      bind[ListOfSchemesConnector].toInstance(listOfSchemesConnector)

    )
    .build()

  override def beforeEach(): Unit = {
    when(minimalPsaConnector.getMinimalPspDetails()(using any(), any()))
      .thenReturn(Future.successful(minimalPsaDetails))
    when(frontendAppConfig.pspDetailsUrl)
      .thenReturn("http://localhost:8208/pension-scheme-practitioner/practitioner-details")
    when(frontendAppConfig.pspDeregisterCompanyUrl)
      .thenReturn("http://localhost:8208/pension-scheme-practitioner/remove-psp/remove-company")
    when(frontendAppConfig.enableMembersProtectionsEnhancements)
      .thenReturn(false)
    when(frontendAppConfig.checkMembersProtectionsEnhancementsUrl)
      .thenReturn("/members-protections-and-enhancements/start")

    super.beforeEach()
  }

  def service: PspDashboardService = new PspDashboardService(frontendAppConfig, minimalPsaConnector)

  "getTiles" must {
    "return tiles with relevant links when all possible links are displayed" in {
      service.getTiles(pspId, minimalPsaDetails) mustBe tiles
    }

    "include 'check-member-protections' link when flag is true" in {
      when(frontendAppConfig.enableMembersProtectionsEnhancements).thenReturn(true)

      service.getTiles(pspId, minimalPsaDetails) mustBe
        Seq(schemeCard(baseLinks ++ mpeLink), practitionerCard)
    }

  }
}

object PspDashboardServiceSpec extends SpecBase with MockitoSugar {

  val pspName: String = "John Doe"
  private val pspId = "00000000"

  def minimalPsaDetails: MinimalPSAPSP = MinimalPSAPSP("test@test.com", isPsaSuspended = false, Some("Org Name"), None,
    rlsFlag = false, deceasedFlag = false)

  private val practitionerCard: CardViewModel =
    CardViewModel(
      id = "practitioner-card",
      heading = Message("messages__pspDashboard__details_heading"),
      subHeadings = Seq(
        CardSubHeading(
          subHeading = Message("messages__pspDashboard__psp_id"),
          subHeadingClasses = "heading-small card-sub-heading",
          subHeadingParams = Seq(
            CardSubHeadingParam(
              subHeadingParam = pspId,
              subHeadingParamClasses = "font-small bold")))),
      links = Seq(
        Link("pspLink", frontendAppConfig.pspDetailsUrl, Message("messages__pspDashboard__psp_change")),
        Link("deregister-link", frontendAppConfig.pspDeregisterCompanyUrl, Message("messages__pspDashboard__psp_deregister"))
      )
    )

  val baseLinks =
    Seq(Link(
      id = "search-schemes",
      url ="/manage-pension-schemes/list-psp",
      linkText = Message("messages__pspDashboard__search_scheme")
    ))

  val mpeLink =
    Seq(Link(
      id = "check-member-protections",
      url ="/members-protections-and-enhancements/start",
      linkText =  Message("messages__pspDashboard__check_member_protections")
    ))

  private def schemeCard(links: Seq[Link] = baseLinks) = {
    CardViewModel(
      id = "scheme-card",
      heading = "Pension schemes",
      links = links,
    )
  }

  private val tiles: Seq[CardViewModel] = Seq(schemeCard(), practitionerCard)

}




