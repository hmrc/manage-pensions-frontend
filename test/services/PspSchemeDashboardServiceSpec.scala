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
import connectors.admin.MinimalConnector
import models.{EROverview, Link}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.twirl.api.Html
import testhelpers.CommonBuilders
import viewmodels.Message.Literal

import java.time.LocalDate

class PspSchemeDashboardServiceSpec extends SpecBase with MockitoSugar {

  private val mockAppConfig = mock[FrontendAppConfig]
  private val mockMinimalConnector = mock[MinimalConnector]
  private val service = new PspSchemeDashboardService(mockAppConfig, mockMinimalConnector)

  "getTiles" must {
    "add PSR, QROPS and IHTP links when subheading is present and feature flags are enabled" in {
      when(mockAppConfig.aftOverviewHtmlUrl).thenReturn("dummy")
      when(mockAppConfig.eventReportingOverviewHtmlUrl).thenReturn("dummy")
      when(mockAppConfig.psrOverviewUrl).thenReturn("dummy")
      when(mockAppConfig.qropsOverviewUrl).thenReturn("dummy")
      when(mockAppConfig.ihtpUrl).thenReturn("dummy")
      when(mockAppConfig.enableQROPSUrl).thenReturn(true)
      when(mockAppConfig.enableIHTPLink).thenReturn(true)

      val overview = EROverview(
        periodStartDate = LocalDate.of(2022, 4, 6),
        periodEndDate = LocalDate.of(2023, 4, 5),
        ntfDateOfIssue = Some(LocalDate.of(2024, 4, 6)),
        psrDueDate = Some(LocalDate.of(2024, 4, 6)),
        psrReportType = Some("PSP")
      )

      val result = service.getTiles(
        erHtml = Html(""),
        srn = "S2400000005",
        pstr = "pstr",
        openDate = None,
        loggedInPsp = CommonBuilders.pspDetails,
        clientReference = None,
        seqErOverview = Seq(overview)
      ).head

      result.links mustBe Seq(
        Link("aft-view-link", "dummy", Literal("Accounting for Tax (AFT) return"), None, None),
        Link("psr-view-details", "dummy", Literal("Pension scheme return"), None, None),
        Link(
          "qrops-view-details",
          "dummy",
          Literal("Report a transfer to a qualifying recognised overseas pension scheme"),
          None,
          None
        ),
        Link("ihtp-view-details", "dummy", Literal("Report Inheritance Tax on a pension"), None, None)
      )
    }
  }
}
