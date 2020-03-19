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

package connectors.aft

import java.time.LocalDate

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, badRequest, equalTo, get, urlEqualTo}
import models.AFTOverview
import org.scalatest.{AsyncWordSpec, MustMatchers, OptionValues}
import play.api.http.Status
import play.api.libs.json.{JsNumber, Json}
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier}
import utils.{DateHelper, WireMockHelper}

class AFTConnectorSpec  extends AsyncWordSpec with MustMatchers with WireMockHelper with OptionValues {

  override protected def portConfigKey: String = "microservice.services.pension-scheme-accounting-for-tax.port"

  protected implicit val hc: HeaderCarrier = HeaderCarrier()
  protected val aftOverview: String = "/pension-scheme-accounting-for-tax/get-aft-overview"

  private val validAftOverviewResponse = Json.arr(
    Json.obj(
      "periodStartDate"-> "2028-04-01",
      "periodEndDate"-> "2028-06-30",
      "numberOfVersions"->  JsNumber(1),
      "submittedVersionAvailable"-> false,
      "compiledVersionAvailable"-> true
    ),
    Json.obj(
      "periodStartDate"-> "2022-01-01",
      "periodEndDate"-> "2022-03-31",
      "numberOfVersions"-> JsNumber(1),
      "submittedVersionAvailable"-> true,
      "compiledVersionAvailable"-> false
    )
  ).toString()
  
  val aftOverviewModel = Seq(
    AFTOverview(
      periodStartDate = LocalDate.of(2028,4,1),
      periodEndDate = LocalDate.of(2028,6,30),
      numberOfVersions = 1,
      submittedVersionAvailable = false,
      compiledVersionAvailable = true
    ),
    AFTOverview(
      periodStartDate = LocalDate.of(2022,1,1),
      periodEndDate = LocalDate.of(2022,3,31),
      numberOfVersions = 1,
      submittedVersionAvailable = true,
      compiledVersionAvailable = false
    )
  )

  val pstr = "pstr"

  "getAftOverview" must {

    "return the AFTOverview for a valid request/response with correct dates" in {

      DateHelper.setDate(Some(LocalDate.of(2028, 5, 23)))

      server.stubFor(
        get(urlEqualTo(aftOverview))
          .withHeader("pstr", equalTo(pstr))
          .withHeader("startDate", equalTo("2022-01-01"))
          .withHeader("endDate", equalTo("2028-06-30"))
          .willReturn(
            aResponse()
              .withStatus(Status.OK)
              .withHeader("Content-Type", "application/json")
              .withBody(validAftOverviewResponse)
          )
      )

      val connector = injector.instanceOf[AFTConnector]

      connector.getAftOverview(pstr).map(aftOverview =>
        aftOverview mustBe aftOverviewModel
      )

    }

    "throw BadRequestException for a 400 INVALID_PSTR response" in {

      server.stubFor(
        get(urlEqualTo(aftOverview))
          .withHeader("pstr", equalTo(pstr))
          .withHeader("startDate", equalTo("2022-01-01"))
          .withHeader("endDate", equalTo("2028-06-30"))
          .willReturn(
            badRequest
              .withHeader("Content-Type", "application/json")
              .withBody(errorResponse("INVALID_PSTR"))
          )
      )

      val connector = injector.instanceOf[AFTConnector]
      recoverToSucceededIf[BadRequestException] {
        connector.getAftOverview(pstr)
      }
    }

    "throw BadRequestException for a 400 INVALID_REPORT_TYPE response" in {

      server.stubFor(
        get(urlEqualTo(aftOverview))
          .withHeader("pstr", equalTo(pstr))
          .withHeader("startDate", equalTo("2022-01-01"))
          .withHeader("endDate", equalTo("2028-06-30"))
          .willReturn(
            badRequest
              .withHeader("Content-Type", "application/json")
              .withBody(errorResponse("INVALID_REPORT_TYPE"))
          )
      )
      val connector = injector.instanceOf[AFTConnector]

      recoverToSucceededIf[BadRequestException] {
        connector.getAftOverview(pstr)
      }

    }

    "throw BadRequestException for a 400 INVALID_FROM_DATE response" in {

      server.stubFor(
        get(urlEqualTo(aftOverview))
          .willReturn(
            badRequest
              .withHeader("Content-Type", "application/json")
              .withBody(errorResponse("INVALID_FROM_DATE"))
          )
      )
      val connector = injector.instanceOf[AFTConnector]

      recoverToSucceededIf[BadRequestException] {
        connector.getAftOverview(pstr)
      }

    }

    "throw BadRequest for a 400 INVALID_TO_DATE response" in {

      server.stubFor(
        get(urlEqualTo(aftOverview))
          .willReturn(
            badRequest
              .withHeader("Content-Type", "application/json")
              .withBody(errorResponse("INVALID_TO_DATE"))
          )
      )
      val connector = injector.instanceOf[AFTConnector]

      recoverToSucceededIf[BadRequestException] {
        connector.getAftOverview(pstr)
      }

    }
  }

  def errorResponse(code: String): String = {
    Json.stringify(
      Json.obj(
        "code" -> code,
        "reason" -> s"Reason for $code"
      )
    )
  }

}