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
import play.api.libs.json.Json
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier}
import utils.WireMockHelper

class PsaDeregistrationConnectorSpec extends AsyncFlatSpec with Matchers with WireMockHelper with Checkers {

  import PsaDeregistrationConnectorSpec._
  override protected def portConfigKey: String = "microservice.services.pension-administrator.port"

  "Delete" should "return successful following a successful deletion" in {
    server.stubFor(
      delete(urlEqualTo(deregisterUrl))
        .willReturn(
          aResponse()
            .withStatus(Status.NO_CONTENT)
        )
    )

    val connector = injector.instanceOf[PsaDeregistrationConnector]

    connector.deregister(psaId).map {
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

    val connector = injector.instanceOf[PsaDeregistrationConnector]
    recoverToSucceededIf[BadRequestException] {
      connector.deregister(psaId)
    }
  }
}


object PsaDeregistrationConnectorSpec {
  implicit val hc : HeaderCarrier = HeaderCarrier()

  private val psaId = "238DAJFASS"
  private val deregisterUrl = s"/pension-administrator/deregister-psa/$psaId"

  def errorResponse(code: String): String = {
    Json.stringify(
      Json.obj(
        "code" -> code,
        "reason" -> s"Reason for $code"
      )
    )
  }
}

