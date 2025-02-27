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
import models.SchemeReferenceNumber
import models.psp.UpdateClientReferenceRequest
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.Checkers
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier}
import utils.WireMockHelper

import java.time.LocalDateTime

class UpdateClientReferenceConnectorSpec extends AsyncFlatSpec with Matchers with WireMockHelper with Checkers {

  import UpdateClientReferenceConnectorSpec._

  override protected def portConfigKey: String = "microservice.services.pension-administrator.port"

  "updateClientReference" should "return successfully following a successful status" in {

    server.stubFor(
      post(urlEqualTo(s"$updateClientReferenceUrl/${srn.id}"))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withBody(Json.stringify(Json.obj("status" -> "OK", "processingDate" -> LocalDateTime.now())))
        )
    )

    val connector = injector.instanceOf[UpdateClientReferenceConnector]

    connector.updateClientReference(clientReferenceRequest, "Added", srn).map(
      _ =>
        server.findAll(postRequestedFor(urlEqualTo(s"$updateClientReferenceUrl/${srn.id}"))).size() shouldBe 1
    )

  }

  it should "throw BadRequestException for a Bad Request (INVALID_PAYLOAD) response" in {
    server.stubFor(
      post(urlEqualTo(s"$updateClientReferenceUrl/${srn.id}"))
        .willReturn(
          aResponse()
            .withStatus(BAD_REQUEST)
        )
    )

    val connector = injector.instanceOf[UpdateClientReferenceConnector]

    recoverToSucceededIf[BadRequestException] {
      connector.updateClientReference(clientReferenceRequest, "Added", srn)
    } map {
      _ =>
        server.findAll(postRequestedFor(urlEqualTo(s"$updateClientReferenceUrl/${srn.id}"))).size() shouldBe 1
    }
  }

}

object UpdateClientReferenceConnectorSpec {

  private val updateClientReferenceUrl = "/pension-administrator/updateClientReference"

  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()
  private val pstr = "test-pstr"
  private val psaId = "A7654321"
  private val pspId = "00000000"
  private val clientRef = "clientRef"
  private val srn = SchemeReferenceNumber("S2400000041")

  private val clientReferenceRequest = UpdateClientReferenceRequest(
    pstr,
    psaId,
    pspId,
    Some(clientRef)
  )

}


