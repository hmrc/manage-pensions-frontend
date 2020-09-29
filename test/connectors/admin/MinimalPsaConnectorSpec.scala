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

package connectors.admin

import base.JsonFileReader
import com.github.tomakehurst.wiremock.client.WireMock._
import models._
import org.scalatest.{AsyncFlatSpec, Matchers}
import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier}
import utils.WireMockHelper

class MinimalPsaConnectorSpec extends AsyncFlatSpec with Matchers with WireMockHelper {

  import MinimalPsaConnectorSpec._

  override protected def portConfigKey: String = "microservice.services.pension-administrator.port"

  "getMinimalPsaDetails" should "return the MinimalPsa for a valid request/response" in {

    server.stubFor(
      get(urlEqualTo(minimalPsaDetailsUrl))
        .withHeader("psaId", equalTo(psaId))
        .willReturn(
          aResponse()
            .withStatus(Status.OK)
            .withHeader("Content-Type", "application/json")
            .withBody(validMinimalPsaDetailsResponse)
        )
    )

    val connector = injector.instanceOf[MinimalPsaConnector]

    connector.getMinimalPsaDetails(psaId).map(psa =>
      psa shouldBe expectedResponse
    )

  }

  it should "throw BadRequestException for a 400 INVALID_PSAID response" in {

    server.stubFor(
      get(urlEqualTo(minimalPsaDetailsUrl))
        .withHeader("psaId", equalTo(psaId))
        .willReturn(
          badRequest
            .withHeader("Content-Type", "application/json")
            .withBody(errorResponse("INVALID_PSAID"))
        )
    )

    val connector = injector.instanceOf[MinimalPsaConnector]
    recoverToSucceededIf[BadRequestException] {
      connector.getMinimalPsaDetails(psaId)
    }
  }

  it should "throw BadRequest for a 400 INVALID_CORRELATIONID response" in {

    server.stubFor(
      get(urlEqualTo(minimalPsaDetailsUrl))
        .willReturn(
          badRequest
            .withHeader("Content-Type", "application/json")
            .withBody(errorResponse("INVALID_CORRELATIONID"))
        )
    )
    val connector = injector.instanceOf[MinimalPsaConnector]

    recoverToSucceededIf[BadRequestException] {
      connector.getMinimalPsaDetails(psaId)
    }
  }

}

object MinimalPsaConnectorSpec extends JsonFileReader {

  private val psaId = "A1234567"
  private val minimalPsaDetailsUrl = s"/pension-administrator/get-minimal-psa"

  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  private val validMinimalPsaDetailsResponse = readJsonFromFile("/data/validMinimalPsaDetails.json").toString()

  def errorResponse(code: String): String = {
    Json.stringify(
      Json.obj(
        "code" -> code,
        "reason" -> s"Reason for $code"
      )
    )
  }

  private val email = "test@test.com"

  private val expectedResponse = MinimalPSAPSP(email,false,None,Some(IndividualDetails("First",Some("Middle"),"Last")))
}





