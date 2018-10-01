/*
 * Copyright 2018 HM Revenue & Customs
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

import base.JsonFileReader
import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.{AsyncFlatSpec, Matchers}
import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier}
import utils.{MockDataHelper, WireMockHelper}

class SchemeDetailsConnectorSpec extends AsyncFlatSpec with Matchers with WireMockHelper {

  import SchemeDetailsConnectorSpec._


  override protected def portConfigKey: String = "microservice.services.pensions-scheme.port"

  "getSchemeDetails" should "return the SchemeDetails for a valid request/response" in {

    server.stubFor(
      get(urlEqualTo(schemeDetailsUrl))
        .withHeader("schemeIdType", equalTo(schemeIdType))
        .withHeader("idNumber", equalTo(idNumber))
        .willReturn(
          aResponse()
            .withStatus(Status.OK)
            .withHeader("Content-Type", "application/json")
            .withBody(validSchemeDetailsResponse)
        )
    )

    val connector = injector.instanceOf[SchemeDetailsConnector]

    connector.getSchemeDetails(schemeIdType, idNumber).map(schemeDetails =>
      schemeDetails shouldBe psaSchemeDetailsResponse
    )

  }

  it should "throw BadRequestException for a 400 INVALID_IDTYPE response" in {

    server.stubFor(
      get(urlEqualTo(schemeDetailsUrl))
        .withHeader("schemeIdType", equalTo(schemeIdType))
        .withHeader("idNumber", equalTo(idNumber))
        .willReturn(
          badRequest
            .withHeader("Content-Type", "application/json")
            .withBody(errorResponse("INVALID_IDTYPE"))
        )
    )

    val connector = injector.instanceOf[SchemeDetailsConnector]
    recoverToSucceededIf[BadRequestException] {
      connector.getSchemeDetails(schemeIdType, idNumber)
    }
  }

  it should "throw BadRequestException for a 400 INVALID_SRN response" in {

    server.stubFor(
      get(urlEqualTo(schemeDetailsUrl))
        .withHeader("schemeIdType", equalTo(schemeIdType))
        .withHeader("idNumber", equalTo(idNumber))
        .willReturn(
          badRequest
            .withHeader("Content-Type", "application/json")
            .withBody(errorResponse("INVALID_SRN"))
        )
    )
    val connector = injector.instanceOf[SchemeDetailsConnector]

    recoverToSucceededIf[BadRequestException] {
      connector.getSchemeDetails(schemeIdType, idNumber)
    }

  }
  it should "throw BadRequestException for a 400 INVALID_PSTR response" in {

    server.stubFor(
      get(urlEqualTo(schemeDetailsUrl))
        .willReturn(
          badRequest
            .withHeader("Content-Type", "application/json")
            .withBody(errorResponse("INVALID_PSTR"))
        )
    )
    val connector = injector.instanceOf[SchemeDetailsConnector]

    recoverToSucceededIf[BadRequestException] {
      connector.getSchemeDetails(schemeIdType, idNumber)
    }

  }

  it should "throw BadRequest for a 400 INVALID_CORRELATIONID response" in {

    server.stubFor(
      get(urlEqualTo(schemeDetailsUrl))
        .willReturn(
          badRequest
            .withHeader("Content-Type", "application/json")
            .withBody(errorResponse("INVALID_CORRELATIONID"))
        )
    )
    val connector = injector.instanceOf[SchemeDetailsConnector]

    recoverToSucceededIf[BadRequestException] {
      connector.getSchemeDetails(schemeIdType, idNumber)
    }

  }

}

object SchemeDetailsConnectorSpec extends JsonFileReader with MockDataHelper {

  private val schemeIdType = "pstr"
  private val idNumber = "00000000AA"
  private val schemeDetailsUrl = s"/pensions-scheme/scheme"

  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  private val validSchemeDetailsResponse = readJsonFromFile("/data/validSchemeDetailsResponse.json").toString()

  def errorResponse(code: String): String = {
    Json.stringify(
      Json.obj(
        "code" -> code,
        "reason" -> s"Reason for $code"
      )
    )
  }

}


