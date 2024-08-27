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

package connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import connectors.admin.ToggleDetails
import models.EROverview
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsArray, JsResultException, Json}
import uk.gov.hmrc.http._
import utils.{Enumerable, WireMockHelper}

import java.time.LocalDate

class PensionSchemeReturnConnectorSpec
  extends AsyncWordSpec
    with Matchers
    with WireMockHelper
    with Enumerable.Implicits {

  private val pstr = "87219363YN"

  private implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  override protected def portConfigKey: String = "microservice.services.pensions-scheme-return.port"

  private def application: Application =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.pension-scheme-event-reporting.port" -> server.port
      )
      .build()

  private val getFeatureTogglePath = "/admin/get-toggle"

  private lazy val connector: PensionSchemeReturnConnector = injector.instanceOf[PensionSchemeReturnConnector]
  val startDate = "2022-04-06"
  val endDate = "2023-04-05"
  private val getOverviewUrl = s"/pension-scheme-return/psr/overview/$pstr?fromDate=$startDate&toDate=$endDate"

  "getOverview" must {
    "return the seq of overviewDetails returned from BE" in {
      val erOverviewResponseJson: JsArray = Json.arr(
        Json.obj(
          "periodStartDate" -> "2022-04-06",
          "periodEndDate" -> "2023-04-05",
          "ntfDateOfIssue" ->  "2024-12-06",
          "psrDueDate" ->  "2025-03-31",
          "psrReportType" ->  "Standard"
        ),
        Json.obj(
          "periodStartDate" -> "2022-04-06",
          "periodEndDate" -> "2023-04-05",
          "psrDueDate" ->  "2025-03-31",
          "psrReportType" ->  "PSA"
        )
      )

      val overview1 = EROverview(
        LocalDate.of(2022, 4, 6),
        LocalDate.of(2023, 4, 5),
        Some(LocalDate.of(2024, 12, 6)),
        Some(LocalDate.of(2025, 3, 31)),
        Some("Standard"))

      val overview2 =  EROverview(
        LocalDate.of(2022, 4, 6),
        LocalDate.of(2023, 4, 5),
        None,
        Some(LocalDate.of(2025, 3, 31)),
        Some("PSA"))

      val erOverview = Seq(overview1, overview2)

      server.stubFor(
        get(urlEqualTo(getOverviewUrl))
          .willReturn(
            ok
              .withHeader("Content-Type", "application/json")
              .withBody(erOverviewResponseJson.toString())
          )
      )
      connector.getOverview(pstr, "ER", "2022-04-06", "2023-04-05").map { response =>
        response mustBe erOverview
      }
    }

    "return JsResultException when the backend has returned errors" in {
      val erOverviewResponseJson: JsArray = Json.arr(
        Json.obj(
          "periodStartDate" -> "2022-04-06"
        )
      )

      server.stubFor(
        get(urlEqualTo(getOverviewUrl))
          .willReturn(
            ok
              .withHeader("Content-Type", "application/json")
              .withBody(erOverviewResponseJson.toString())
          )
      )

      recoverToSucceededIf[JsResultException] {
        connector.getOverview(pstr, "ER", "2022-04-06", "2023-04-05")
      }
    }

    "return BadRequestException when the backend has returned bad request response" in {
      server.stubFor(
        get(urlEqualTo(getOverviewUrl))
          .willReturn(
            badRequest
              .withHeader("Content-Type", "application/json")
          )
      )

      recoverToSucceededIf[HttpException] {
        connector.getOverview(pstr, "ER", "2022-04-06", "2023-04-05")
      }
    }
  }

}
