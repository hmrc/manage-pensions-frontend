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

package connectors.scheme

import base.JsonFileReader
import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier}
import utils.{UserAnswers, WireMockHelper}

class SchemeDetailsConnectorSpec extends AsyncFlatSpec with Matchers with WireMockHelper {

  import SchemeDetailsConnectorSpec._


  override protected def portConfigKey: String = "microservice.services.pensions-scheme.port"

  "getSchemeDetails" should "return the SchemeDetails for a valid request/response" in {
    val jsonResponse = """{"abc":"def"}"""
    server.stubFor(
      get(urlEqualTo(schemeDetailsUrl))
        .withHeader("psaId", equalTo(psaId))
        .withHeader("idNumber", equalTo(idNumber))
        .withHeader("schemeIdType", equalTo(schemeIdType))
        .willReturn(
          aResponse()
            .withStatus(Status.OK)
            .withHeader("Content-Type", "application/json")
            .withBody(jsonResponse)
        )
    )

    val connector = injector.instanceOf[SchemeDetailsConnector]

    connector.getSchemeDetails(
      psaId = psaId,
      idNumber = idNumber,
      schemeIdType = schemeIdType
    ) map (schemeDetails =>
      schemeDetails shouldBe UserAnswers(Json.parse(jsonResponse))
      )

  }

  it should "throw BadRequestException for a 400 INVALID_IDTYPE response" in {

    server.stubFor(
      get(urlEqualTo(schemeDetailsUrl))
        .withHeader("psaId", equalTo(psaId))
        .withHeader("idNumber", equalTo(idNumber))
        .withHeader("schemeIdType", equalTo(schemeIdType))
        .willReturn(
          badRequest
            .withHeader("Content-Type", "application/json")
            .withBody(errorResponse("INVALID_IDTYPE"))
        )
    )

    val connector = injector.instanceOf[SchemeDetailsConnector]
    recoverToSucceededIf[BadRequestException] {
      connector.getSchemeDetails(
        psaId = psaId,
        idNumber = idNumber,
        schemeIdType = schemeIdType
      )
    }
  }

  it should "throw BadRequestException for a 400 INVALID_SRN response" in {

    server.stubFor(
      get(urlEqualTo(schemeDetailsUrl))
        .withHeader("psaId", equalTo(psaId))
        .withHeader("idNumber", equalTo(idNumber))
        .withHeader("schemeIdType", equalTo(schemeIdType))
        .willReturn(
          badRequest
            .withHeader("Content-Type", "application/json")
            .withBody(errorResponse("INVALID_SRN"))
        )
    )
    val connector = injector.instanceOf[SchemeDetailsConnector]

    recoverToSucceededIf[BadRequestException] {
      connector.getSchemeDetails(
        psaId = psaId,
        idNumber = idNumber,
        schemeIdType = schemeIdType
      )
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
      connector.getSchemeDetails(
        psaId = psaId,
        idNumber = idNumber,
        schemeIdType = schemeIdType
      )
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
      connector.getSchemeDetails(
        psaId = psaId,
        idNumber = idNumber,
        schemeIdType = schemeIdType
      )
    }

  }

  "getSchemeDetailsRefresh" should "return the getSchemeDetailsRefresh for a valid request/response" in {
    val jsonResponse = """{"abc":"def"}"""
    server.stubFor(
      get(urlEqualTo(schemeDetailsUrl))
        .withHeader("psaId", equalTo(psaId))
        .withHeader("idNumber", equalTo(idNumber))
        .withHeader("schemeIdType", equalTo(schemeIdType))
        .withHeader("refreshData", equalTo("true"))
        .willReturn(
          aResponse()
            .withStatus(Status.OK)
            .withHeader("Content-Type", "application/json")
            .withBody(jsonResponse)
        )
    )

    val connector = injector.instanceOf[SchemeDetailsConnector]

    connector.getSchemeDetailsRefresh(
      psaId = psaId,
      idNumber = idNumber,
      schemeIdType = schemeIdType
    ) map (schemeDetails =>
      schemeDetails shouldBe()
      )

  }

  it should "throw BadRequestException for a 400 INVALID_IDTYPE response" in {

    server.stubFor(
      get(urlEqualTo(schemeDetailsUrl))
        .withHeader("psaId", equalTo(psaId))
        .withHeader("idNumber", equalTo(idNumber))
        .withHeader("schemeIdType", equalTo(schemeIdType))
        .withHeader("refreshData", equalTo("true"))
        .willReturn(
          badRequest
            .withHeader("Content-Type", "application/json")
            .withBody(errorResponse("INVALID_IDTYPE"))
        )
    )

    val connector = injector.instanceOf[SchemeDetailsConnector]
    recoverToSucceededIf[BadRequestException] {
      connector.getSchemeDetailsRefresh(
        psaId = psaId,
        idNumber = idNumber,
        schemeIdType = schemeIdType
      )
    }
  }

}

object SchemeDetailsConnectorSpec extends JsonFileReader {
  private val psaId = "0000"
  private val schemeIdType = "pstr"
  private val idNumber = "00000000AA"
  private val schemeDetailsUrl = s"/pensions-scheme/scheme/$idNumber"

  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  def errorResponse(code: String): String = {
    Json.stringify(
      Json.obj(
        "code" -> code,
        "reason" -> s"Reason for $code"
      )
    )
  }

}


