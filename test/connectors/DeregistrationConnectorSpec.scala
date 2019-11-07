/*
 * Copyright 2019 HM Revenue & Customs
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
import org.scalatest.prop.Checkers
import org.scalatest.{AsyncFlatSpec, Matchers}
import play.api.http.Status
import play.api.libs.json.{JsBoolean, JsResultException, JsString, Json}
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, Upstream5xxResponse}
import utils.WireMockHelper

class DeregistrationConnectorSpec extends AsyncFlatSpec with Matchers with WireMockHelper with Checkers {

  import DeregistrationConnectorSpec._
  override protected def portConfigKey: String = "microservice.services.pension-administrator.port"

  "Delete" should "return successful following a successful deletion" in {
    server.stubFor(
      delete(urlEqualTo(deregisterUrl))
        .willReturn(
          aResponse()
            .withStatus(Status.NO_CONTENT)
        )
    )

    val connector = injector.instanceOf[DeregistrationConnector]

    connector.stopBeingPSA(psaId).map {
      _ => server.findAll(deleteRequestedFor(urlEqualTo(deregisterUrl))).size() shouldBe 1
    }
  }

  it should "throw BadRequestException for a 400 INVALID_PAYLOAD response" in {

    server.stubFor(
      delete(urlEqualTo(deregisterUrl))
        .willReturn(
          badRequest
            .withHeader("Content-Type", "application/json")
            .withBody(errorResponse("INVALID_PAYLOAD"))
        )
    )

    val connector = injector.instanceOf[DeregistrationConnector]
    recoverToSucceededIf[BadRequestException] {
      connector.stopBeingPSA(psaId)
    }
  }

  "canDeRegister" should "return the boolean true/false for a valid response" in {

    server.stubFor(
      get(urlEqualTo(canRegisterUrl))
        .willReturn(
          ok(Json.stringify(JsBoolean(true)))
        )
    )

    val connector = injector.instanceOf[DeregistrationConnector]

    connector.canDeRegister(psaId).map(response =>
      response shouldBe true
    )
  }

  it should "throw JsResultException is the data returned is not boolean" in {
    server.stubFor(
      get(urlEqualTo(canRegisterUrl))
        .willReturn(
          ok(Json.stringify(JsString("invalid data")))
        )
    )

    val connector = injector.instanceOf[DeregistrationConnector]

    recoverToSucceededIf[JsResultException] {
      connector.canDeRegister(psaId)
    }
  }

  it should "throw a Upstream5xxResponse if service unavailable is returned" in {
    server.stubFor(
      get(urlEqualTo(canRegisterUrl))
        .willReturn(
          aResponse()
            .withStatus(Status.SERVICE_UNAVAILABLE)
        )
    )
    val connector = injector.instanceOf[DeregistrationConnector]

    recoverToSucceededIf[Upstream5xxResponse] {
      connector.canDeRegister(psaId)
    }
  }
}


object DeregistrationConnectorSpec {
  implicit val hc : HeaderCarrier = HeaderCarrier()

  private val psaId = "238DAJFASS"
  private val deregisterUrl = s"/pension-administrator/deregister-psa/$psaId"
  private val canRegisterUrl = s"/pension-administrator/can-deregister/$psaId"

  def errorResponse(code: String): String = {
    Json.stringify(
      Json.obj(
        "code" -> code,
        "reason" -> s"Reason for $code"
      )
    )
  }
}

