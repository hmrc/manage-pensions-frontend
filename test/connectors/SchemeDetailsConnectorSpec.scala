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
import models._
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
      schemeDetails shouldBe expectedResponse
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

  private val schemeDetails = SchemeDetails("S9000000000", "00000000AA", "Pending", "Benefits Scheme", Some(true),
    Some("A single trust under which all of the assets are held for the benefit of all members of the scheme"),
    Some(" "), Some(true), "0", "0", true, true, "Money Purchase benefits only (defined contribution)", "AD", true, true,
    Some("Aviva Insurance"), Some(" "), Some(address), Some(contactDetails))

  private val indEstablisher = Individual(personDetails,Some("AA999999A"),Some("retxgfdg"),Some("1234567892"),
    Some("asdgdgdsg"),indEstAddress,indEstcontactDetails,indEstPrevAdd)

  private val compEstablisher = CompanyEstablisher("abc organisation",Some("7897700000"),Some("reason forutr"),Some("sdfsfs"),
      Some("crn no reason"),Some("789770000"),Some("9999"),Some(true),comEstAddress,comEstcontactDetails,Some(comEstPrevAdd),None)

  private val establisherDetails = EstablisherDetails(Some(List(indEstablisher)), Some(List(compEstablisher)), None)

  private val psaDetails1 = PsaDetails("A0000000",Some("partnetship name"),Some("Taylor"),Some("Middle"),Some("Rayon"),Some("Primary"),Some("1978-03-22"))
  private val psaDetails2 = PsaDetails("A0000001",Some("partnetship name 1"),Some("Smith"),Some("A"),Some("Tony"),Some("Primary"),Some("1977-03-22"))

  private val expectedResponse = PsaSchemeDetails(PensionsScheme(schemeDetails, Some(establisherDetails), None, Some(Seq(psaDetails1, psaDetails2))))

}


