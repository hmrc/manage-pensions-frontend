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
import utils.WireMockHelper

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

object SchemeDetailsConnectorSpec extends JsonFileReader {

  private val schemeIdType = "pstr"
  private val idNumber = "00000000AA"
  private val schemeDetailsUrl = s"/pension-administrator/scheme"

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

  private val address = Address(false, "Telford1", "Telford2", Some("Telford13"), Some("Telford14"), Some("TF3 4ER"), "GB")
  private val indEstAddress = Address(false,"addressline1","addressline2",Some("addressline3"),Some("addressline4"),Some("TF3 5TR"),"GB")
  private val comEstAddress = Address(true,"line1","line2",Some("line3"),Some("line4"),Some("LE45RT"),"GB")
  private val contactDetails = ContactDetails("0044-09876542312", Some("0044-09876542312"), Some("0044-09876542312"), "abc@hmrc.gsi.gov.uk")
  private val indEstcontactDetails = ContactDetails("0044-09876542334",Some("0044-09876542312"),Some("0044-09876542312"),"aaa@gmail.com")
  private val comEstcontactDetails = ContactDetails("0044-09876542312",Some("0044-09876542312"),Some("0044-09876542312"),"abcfe@hmrc.gsi.gov.uk")
  private val indEstPrevAdd = PreviousAddressDetails(true, Some(Address(true,"sddsfsfsdf","sddsfsdf",Some("sdfdsfsdf"),Some("sfdsfsdf"),Some("456546"),"AD")))
  private val comEstPrevAdd = PreviousAddressDetails(true,Some(Address(true,"addline1","addline2",Some("addline3"),Some("addline4"),Some("ST36TR"),"AD")))
  private val personDetails = PersonDetails(Some("Mr"),"abcdef",Some("fdgdgfggfdg"),"dfgfdgdfg","1955-03-29")

  private val schemeDetails = SchemeDetails("S9000000000", "00000000AA", "Pending", "Benefits Scheme", Some(true),
    Some("A single trust under which all of the assets are held for the benefit of all members of the scheme"),
    Some(" "), Some(true), "0", "0", true, true, "Money Purchase benefits only (defined contribution)", "AD", true, true,
    Some("Aviva Insurance"), Some(" "), Some(address), Some(contactDetails))

  private val indEstablisher = Individual(personDetails,Some("AA999999A"),Some("retxgfdg"),Some("1234567892"),
    Some("asdgdgdsg"),indEstAddress,indEstcontactDetails,indEstPrevAdd)

  private val compEstablisher = CompanyEstablisher("abc organisation",Some("7897700000"),Some("reason forutr"),Some("sdfsfs"),
      Some("crn no reason"),Some("789770000"),Some("9999"),Some(true),comEstAddress,comEstcontactDetails,Some(comEstPrevAdd),None)

  private val establisherDetails = EstablisherDetails(Some(List(indEstablisher)), Some(List(compEstablisher)), None)

  private val psaDetails = PsaDetails("A0000000",Some("partnetship name"),Some("Taylor"),Some("Middle"),Some("Rayon"),Some("Primary"),Some("1978-03-22"))

  private val expectedResponse = PsaSchemeDetails(PensionsScheme(schemeDetails, Some(establisherDetails), None, Some(psaDetails)))

}


