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
import models.{LegacySchemeDetails, ListOfLegacySchemes}
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import utils.WireMockHelper

class MigrationConnectorSpec extends AsyncFlatSpec with Matchers with WireMockHelper with BeforeAndAfterEach {

  import MigrationConnectorSpec._

  override protected def portConfigKey: String = "microservice.services.pensions-scheme-migration.port"

  "migration connector" should "return the List of Schemes for a valid request/response" in {

    server.stubFor(
      get(urlEqualTo(listOfSchemesUrl))
        .withHeader("psaId", equalTo(psaId))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withHeader("Content-Type", "application/json")
            .withBody(Json.stringify(Json.toJson(expectedResponse)))
        )
    )

    val connector = injector.instanceOf[MigrationConnector]

    connector.getListOfLegacySchemes(psaId).map(listOfSchemes =>
      listOfSchemes.toOption.value shouldBe expectedResponse
    )
  }

  it should "return a 400 INVALID_PAYLOAD response" in {

    server.stubFor(
      get(urlEqualTo(listOfSchemesUrl))
        .willReturn(
          badRequest
            .withHeader("Content-Type", "application/json")
            .withBody(invalidPayloadResponse)
        )
    )

    val connector = injector.instanceOf[MigrationConnector]

    connector.getListOfLegacySchemes(psaId).map(listOfSchemes =>
      listOfSchemes.swap.toOption.value.status shouldBe BAD_REQUEST
    )
  }

  it should "return a 500 response" in {

    server.stubFor(
      get(urlEqualTo(listOfSchemesUrl))
        .willReturn(
          aResponse()
            .withStatus(INTERNAL_SERVER_ERROR)
            .withHeader("Content-Type", "application/json")
            .withBody("{}")
        )
    )

    val connector = injector.instanceOf[MigrationConnector]

    connector.getListOfLegacySchemes(psaId).map(listOfSchemes =>
      listOfSchemes.swap.toOption.value.status shouldBe INTERNAL_SERVER_ERROR
    )
  }
}

object MigrationConnectorSpec extends OptionValues {

  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  private val listOfSchemesUrl = "/pensions-scheme-migration/list-of-schemes"
  private val psaId = "A2110001"

  private val schemeDetail =
    LegacySchemeDetails("10000678RE", "2020-10-10", racDac = false, "abcdefghi", "2020-12-12", None)
  private val racDacDetail =
    LegacySchemeDetails("10000678RF", "2020-10-10", racDac = true, "abcdefghi", "2020-12-12", Some("12345678"))

  private val expectedResponse = ListOfLegacySchemes(2, Some(List(schemeDetail, racDacDetail)))

  private val invalidPayloadResponse =
    Json.stringify(
      Json.obj(
        "code" -> "INVALID_PAYLOAD",
        "reason" -> "test-reason"
      )
    )
}
