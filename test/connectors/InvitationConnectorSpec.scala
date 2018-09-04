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
import models.Invitation
import org.scalatest.prop.Checkers
import org.scalatest.{AsyncFlatSpec, Matchers}
import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import utils.WireMockHelper

class InvitationConnectorSpec extends AsyncFlatSpec with Matchers with WireMockHelper with Checkers {

  import InvitationConnectorSpec._

  override protected def portConfigKey: String = "microservice.services.pension-administrator.port"

  "invite" should "return successfully following a successful invite" in {

    server.stubFor(
      post(urlEqualTo(inviteUrl))
        .withRequestBody(equalToJson(requestJson))
        .willReturn(
          aResponse()
            .withStatus(Status.CREATED)
        )
    )

    val connector = injector.instanceOf[InvitationConnector]

    connector.invite(invitation).map(
      _ =>
        server.findAll(postRequestedFor(urlEqualTo(inviteUrl))).size() shouldBe 1
    )

  }

  it should "throw PsaIdInvalidException for a Bad Request (INVALID_PSAID) response" in {

    server.stubFor(
      post(urlEqualTo(inviteUrl))
        .willReturn(
          aResponse()
            .withStatus(Status.BAD_REQUEST)
            .withHeader("Content-Type", "application/json")
            .withBody(invalidPsaIdResponse)
        )
    )

    val connector = injector.instanceOf[InvitationConnector]

    recoverToExceptionIf[PsaIdInvalidException] {
      connector.invite(invitation)
    } map {
      _ =>
        server.findAll(postRequestedFor(urlEqualTo(inviteUrl))).size() shouldBe 1
    }

  }

  it should "throw PsaIdNotFoundException for a Not Found response" in {

    server.stubFor(
      post(urlEqualTo(inviteUrl))
        .willReturn(
          aResponse()
            .withStatus(Status.NOT_FOUND)
        )
    )

    val connector = injector.instanceOf[InvitationConnector]

    recoverToExceptionIf[PsaIdNotFoundException] {
      connector.invite(invitation)
    } map {
      _ =>
        server.findAll(postRequestedFor(urlEqualTo(inviteUrl))).size() shouldBe 1
    }

  }

}

object InvitationConnectorSpec {

  private val inviteUrl = "/pension-administrator/invite"

  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  private val pstr = "test-pstr"
  private val schemeName = "test-scheme-name"
  private val inviterPsaId = "test-inviter-psa-id"
  private val inviteePsaId = "test-invitee-psa-id"
  private val inviteeName = "test-invitee-name"

  private val invitation =
    Invitation(
      pstr,
      schemeName,
      inviterPsaId,
      inviteePsaId,
      inviteeName
    )

  private val requestJson =
    Json.stringify(
      Json.toJson(invitation)
    )

  private val invalidPsaIdResponse =
    Json.stringify(
      Json.obj(
        "code" -> "INVALID_PSAID",
        "reason" -> "Reason for INVALID_PSAID"
      )
    )

}
