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

package connectors.admin

import base.JsonFileReader
import com.github.tomakehurst.wiremock.client.WireMock._
import models._
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier}
import utils.WireMockHelper

class MinimalConnectorSpec extends AsyncFlatSpec with Matchers with WireMockHelper {

  import MinimalConnectorSpec._

  override protected def portConfigKey: String = "microservice.services.pension-administrator.port"

  "getMinimalPsaDetails" should "return the MinimalPsa for a valid request/response" in {

    server.stubFor(
      get(urlEqualTo(minimalPsaDetailsUrl))
        .withHeader("loggedInAsPsa", equalTo("true"))
        .willReturn(
          aResponse()
            .withStatus(Status.OK)
            .withHeader("Content-Type", "application/json")
            .withBody(validMinimalPsaDetailsResponse)
        )
    )

    val connector = injector.instanceOf[MinimalConnector]

    connector.getMinimalPsaDetails().map(psa =>
      psa `shouldBe` expectedResponse
    )

  }

  it should "throw BadRequestException for a 400 INVALID_PSAID response" in {

    server.stubFor(
      get(urlEqualTo(minimalPsaDetailsUrl))
        .withHeader("loggedInAsPsa", equalTo("true"))
        .willReturn(
          badRequest
            .withHeader("Content-Type", "application/json")
            .withBody(errorResponse("INVALID_PSAID"))
        )
    )

    val connector = injector.instanceOf[MinimalConnector]
    recoverToSucceededIf[BadRequestException] {
      connector.getMinimalPsaDetails()
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
    val connector = injector.instanceOf[MinimalConnector]

    recoverToSucceededIf[BadRequestException] {
      connector.getMinimalPsaDetails()
    }
  }

  "retrieveEmailDetails" should "return the email for a valid request/response" in {

    server.stubFor(
      get(urlEqualTo(s"$emailDetailsUrl/${srn.id}"))
        .withHeader("id", equalTo("id"))
        .withHeader("idType" , equalTo("idType"))
        .withHeader("name" , equalTo("name"))
        .willReturn(
          aResponse()
            .withStatus(Status.OK)
            .withHeader("Content-Type", "application/json")
            .withBody(email)
        )
    )

    val connector = injector.instanceOf[MinimalConnector]

    connector.getEmailInvitation("id", "idType", "name", srn).map(psa =>
      psa `shouldBe` Some(email)
    )

  }
  it should "throw PspUserNameNotMatchedException exception when User is not matched" in {

    server.stubFor(
      get(urlEqualTo(s"$emailDetailsUrl/${srn.id}"))
        .withHeader("id", equalTo("id"))
        .withHeader("idType" , equalTo("idType"))
        .withHeader("name" , equalTo("name"))
        .willReturn(
          forbidden()
            .withHeader("Content-Type", "application/json")
            .withBody(errorResponse(pspUserNotMatchedErrorMsg))
        )
    )

    val connector = injector.instanceOf[MinimalConnector]

    recoverToSucceededIf[PspUserNameNotMatchedException] {
      connector.getEmailInvitation("id", "idType", "name", srn)
    }
  }
  it should "throw DelimitedPractitionerException exception when downstream response is forbidden" in {

    server.stubFor(
      get(urlEqualTo(s"$emailDetailsUrl/${srn.id}"))
        .withHeader("id", equalTo("id"))
        .withHeader("idType" , equalTo("idType"))
        .withHeader("name" , equalTo("name"))
        .willReturn(
          forbidden()
            .withHeader("Content-Type", "application/json")
            .withBody(errorResponse(pspDelimitedErrorMsg))
        )
    )

    val connector = injector.instanceOf[MinimalConnector]

    recoverToSucceededIf[DelimitedPractitionerException] {
      connector.getEmailInvitation("id", "idType", "name", srn)
    }
  }
}

object MinimalConnectorSpec extends JsonFileReader {

  private val minimalPsaDetailsUrl = "/pension-administrator/get-minimal-details-self"
  private val emailDetailsUrl = "/pension-administrator/get-email-invitation"

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
  private val srn = SchemeReferenceNumber("S2400000041")
  private val pspUserNotMatchedErrorMsg: String = "Provided user's name doesn't match with stored user's name"
  private val pspDelimitedErrorMsg: String = "DELIMITED_PSPID"
  private val expectedResponse = MinimalPSAPSP(email, isPsaSuspended = false, None, Some(IndividualDetails("First", Some("Middle"), "Last")),
    rlsFlag = false, deceasedFlag = false)
}





