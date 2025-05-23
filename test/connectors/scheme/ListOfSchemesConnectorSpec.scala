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

import com.github.tomakehurst.wiremock.client.WireMock._
import models.{ListOfSchemes, SchemeDetails, SchemeStatus}
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import utils.WireMockHelper

class ListOfSchemesConnectorSpec extends AsyncFlatSpec with Matchers with WireMockHelper with BeforeAndAfterEach {

  import ListOfSchemesConnectorSpec._

  override protected def portConfigKey: String = "microservice.services.pensions-scheme.port"

  "list of schemes connector" should "return the List of Schemes for a valid request/response" in {

    server.stubFor(
      get(urlEqualTo(listOfSchemesUrl))
        .withHeader("idType", equalTo("PSA"))
        .withHeader("idValue", equalTo(psaId))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withHeader("Content-Type", "application/json")
            .withBody(validResponse)
        )
    )

    val connector = injector.instanceOf[ListOfSchemesConnector]

    connector.getListOfSchemes(psaId).map(listOfSchemes =>
      listOfSchemes.toOption.get shouldBe expectedResponse
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

    connector.getListOfSchemes(psaId).map(listOfSchemes =>
      listOfSchemes.left.toOption.get.status shouldBe BAD_REQUEST
    )
  }

  it should "throw InternalServerErrorException for a 500 response" in {

    server.stubFor(
      get(urlEqualTo(listOfSchemesUrl))
        .willReturn(
          aResponse()
            .withStatus(INTERNAL_SERVER_ERROR)
            .withHeader("Content-Type", "application/json")
            .withBody("{}")
        )
    )

    val connector = injector.instanceOf[ListOfSchemesConnector]

    recoverToSucceededIf[InternalServerErrorException] {
      connector.getListOfSchemes(psaId)
    }
    connector.getListOfSchemes(psaId).map(listOfSchemes =>
      listOfSchemes.left.toOption.get.status shouldBe INTERNAL_SERVER_ERROR
    )

  }

}

object ListOfSchemesConnectorSpec extends OptionValues {

  private val listOfSchemesUrl = "/pensions-scheme/list-of-schemes-self"

  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  private val psaId = "A2110001"

  private val validResponse =
    Json.stringify(
      Json.obj(
        "processingDate" -> "2001-12-17T09:30:47Z",
        "totalSchemesRegistered" -> "1",
        "schemeDetails" -> Json.arr(
          Json.obj(
            "name" -> "abcdefghi",
            "referenceNumber" -> "S1000000456",
            "schemeStatus" -> SchemeStatus.Pending.value,
            "openDate" -> "2012-10-10",
            "pstr" -> "10000678RE",
            "relationship" -> "Primary PSA"
          )
        )
      )
    )

  private val schemeDetail = SchemeDetails("abcdefghi", "S1000000456", SchemeStatus.Pending.value, Some("2012-10-10"), None,
    Some("10000678RE"), Some("Primary PSA"), None)

  private val expectedResponse = ListOfSchemes("2001-12-17T09:30:47Z", "1", Some(List(schemeDetail)))

  private val invalidPayloadResponse =
    Json.stringify(
      Json.obj(
        "code" -> "INVALID_PAYLOAD",
        "reason" -> "test-reason"
      )
    )
}
