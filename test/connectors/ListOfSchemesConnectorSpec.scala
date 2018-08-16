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

import com.github.tomakehurst.wiremock.client.WireMock._
import models.{ListOfSchemes, SchemeDetail}
import org.scalatest.{AsyncFlatSpec, Matchers, OptionValues}
import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import utils.WireMockHelper

class ListOfSchemesConnectorSpec extends AsyncFlatSpec with Matchers with WireMockHelper {

  import ListOfSchemesConnectorSpec._

  override protected def portConfigKey: String = "microservice.services.pensions-scheme.port"

  "registerScheme" should "return the List of Schemes for a valid request/response" in {

    server.stubFor(
      get(urlEqualTo(listOfSchemesUrl))
        .withHeader("psaId", equalTo(psaId))
        .willReturn(
          aResponse()
            .withStatus(Status.OK)
            .withHeader("Content-Type", "application/json")
            .withBody(validResponse)
        )
    )

    val connector = injector.instanceOf[ListOfSchemesConnector]

    connector.getListOfSchemes(psaId).map(listOfSchemes =>
      listOfSchemes shouldBe expectedResponse
    )

  }

  it should "throw InvalidPayloadException for a 400 INVALID_PAYLOAD response" in {

    server.stubFor(
      get(urlEqualTo(listOfSchemesUrl))
        .willReturn(
          badRequest
            .withHeader("Content-Type", "application/json")
            .withBody(invalidPayloadResponse)
        )
    )

    val connector = injector.instanceOf[ListOfSchemesConnector]

    recoverToSucceededIf[InvalidPayloadException] {
      connector.getListOfSchemes(psaId)
    }
  }

  it should "throw InvalidCorrelationIdException for a 400 INVALID_CORRELATION_ID response" in {

    server.stubFor(
      get(urlEqualTo(listOfSchemesUrl))
        .willReturn(
          badRequest
            .withHeader("Content-Type", "application/json")
            .withBody(invalidCorrelationIdResponse)
        )
    )

    val connector = injector.instanceOf[ListOfSchemesConnector]

    recoverToSucceededIf[InvalidCorrelationIdException] {
      connector.getListOfSchemes(psaId)
    }

  }

  it should "throw InternalServerErrorException for a 500 response" in {

    server.stubFor(
      get(urlEqualTo(listOfSchemesUrl))
        .willReturn(
          aResponse()
            .withStatus(Status.INTERNAL_SERVER_ERROR)
            .withHeader("Content-Type", "application/json")
            .withBody("{}")
        )
    )

    val connector = injector.instanceOf[ListOfSchemesConnector]

    recoverToSucceededIf[InternalServerErrorException] {
      connector.getListOfSchemes(psaId)
    }

  }

  it should "throw ServiceUnavailableException for a 503 response" in {

    server.stubFor(
      get(urlEqualTo(listOfSchemesUrl))
        .willReturn(
          aResponse()
            .withStatus(Status.SERVICE_UNAVAILABLE)
            .withHeader("Content-Type", "application/json")
            .withBody("{}")
        )
    )

    val connector = injector.instanceOf[ListOfSchemesConnector]

    recoverToSucceededIf[ServiceUnavailableException] {
      connector.getListOfSchemes(psaId)
    }

  }

}

object ListOfSchemesConnectorSpec extends OptionValues {

  private val listOfSchemesUrl = "/pensions-scheme/list-of-schemes"

  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  private val psaId = "A2110001"

  private val validResponse =
    Json.stringify(
      Json.obj(
        "processingDate" -> "2001-12-17T09:30:47Z",
        "totalSchemesRegistered" -> "1",
        "schemeDetail" -> Json.arr(
          Json.obj(
            "name" -> "abcdefghi",
            "referenceNumber" -> "S1000000456",
            "schemeStatus" -> "Pending",
            "openDate" -> "2012-10-10",
            "pstr" -> "10000678RE",
            "relationShip" -> "Primary PSA"
          )
        )
      )
    )

  private val schemeDetail = SchemeDetail("abcdefghi", "S1000000456", "Pending", Some("2012-10-10"),
    Some("10000678RE"), Some("Primary PSA"), None)

  private val expectedResponse = ListOfSchemes("2001-12-17T09:30:47Z", "1", Some(List(schemeDetail)))

  private val invalidPayloadResponse =
    Json.stringify(
      Json.obj(
        "code" -> "INVALID_PAYLOAD",
        "reason" -> "test-reason"
      )
    )

  private val invalidCorrelationIdResponse =
    Json.stringify(
      Json.obj(
        "code" -> "INVALID_CORRELATION_ID",
        "reason" -> "test-reason"
      )
    )
}
