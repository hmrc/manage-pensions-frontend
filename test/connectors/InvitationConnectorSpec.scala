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
import models.{AcceptedInvitation, Invitation}
import org.scalatest.prop.Checkers
import org.scalatest.{AsyncFlatSpec, Matchers}
import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}
import utils.{UserAnswers, WireMockHelper}

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

  "acceptInvite" should "return successfully when user accepts the invite" in {

    server.stubFor(
      post(urlEqualTo(acceptInviteUrl))
        .withRequestBody(equalToJson(Json.stringify(Json.toJson(acceptedInvitation))))
        .willReturn(
          aResponse()
            .withStatus(Status.OK)
        )
    )

    val connector = injector.instanceOf[InvitationConnector]

    connector.acceptInvite(acceptedInvitation) map {
      _ =>
        server.findAll(postRequestedFor(urlEqualTo(acceptInviteUrl))).size() shouldBe 1
    }
  }

  it should "throw PstrInvalidException for a Bad Request (INVALID_PSTR) response" in {
    server.stubFor(
      post(urlEqualTo(acceptInviteUrl))
        .willReturn(
          aResponse()
            .withStatus(Status.BAD_REQUEST)
            .withHeader("Content-Type", "application/json")
            .withBody(invalidResponse("INVALID_PSTR"))
        )
    )

    val connector = injector.instanceOf[InvitationConnector]

    recoverToExceptionIf[PstrInvalidException] {
      connector.acceptInvite(acceptedInvitation)
    } map {
      _ =>
        server.findAll(postRequestedFor(urlEqualTo(acceptInviteUrl))).size() shouldBe 1
    }
  }

  it should "throw InvalidInvitationPayloadException for a Bad Request (INVALID_PAYLOAD) response" in {
    server.stubFor(
      post(urlEqualTo(acceptInviteUrl))
        .willReturn(
          aResponse()
            .withStatus(Status.BAD_REQUEST)
            .withHeader("Content-Type", "application/json")
            .withBody(invalidResponse("INVALID_PAYLOAD"))
        )
    )

    val connector = injector.instanceOf[InvitationConnector]

    recoverToExceptionIf[InvalidInvitationPayloadException] {
      connector.acceptInvite(acceptedInvitation)
    } map {
      _ =>
        server.findAll(postRequestedFor(urlEqualTo(acceptInviteUrl))).size() shouldBe 1
    }
  }

  it should "throw InviteePsaIdInvalidException for a Bad Request (INVALID_INVITEE_PSAID) response" in {
    server.stubFor(
      post(urlEqualTo(acceptInviteUrl))
        .willReturn(
          aResponse()
            .withStatus(Status.FORBIDDEN)
            .withHeader("Content-Type", "application/json")
            .withBody(invalidResponse("INVALID_INVITEE_PSAID"))
        )
    )

    val connector = injector.instanceOf[InvitationConnector]

    recoverToExceptionIf[InviteePsaIdInvalidException] {
      connector.acceptInvite(acceptedInvitation)
    } map {
      _ =>
        server.findAll(postRequestedFor(urlEqualTo(acceptInviteUrl))).size() shouldBe 1
    }
  }

  it should "throw InviterPsaIdInvalidException for a Bad Request (INVALID_INVITER_PSAID) response" in {
    server.stubFor(
      post(urlEqualTo(acceptInviteUrl))
        .willReturn(
          aResponse()
            .withStatus(Status.FORBIDDEN)
            .withHeader("Content-Type", "application/json")
            .withBody(invalidResponse("INVALID_INVITER_PSAID"))
        )
    )

    val connector = injector.instanceOf[InvitationConnector]

    recoverToExceptionIf[InviterPsaIdInvalidException] {
      connector.acceptInvite(acceptedInvitation)
    } map {
      _ =>
        server.findAll(postRequestedFor(urlEqualTo(acceptInviteUrl))).size() shouldBe 1
    }
  }

  it should "throw ActiveRelationshipExistsException for a Bad Request (ACTIVE_RELATIONSHIP_EXISTS) response" in {
    server.stubFor(
      post(urlEqualTo(acceptInviteUrl))
        .willReturn(
          aResponse()
            .withStatus(Status.FORBIDDEN)
            .withHeader("Content-Type", "application/json")
            .withBody(invalidResponse("ACTIVE_RELATIONSHIP_EXISTS"))
        )
    )

    val connector = injector.instanceOf[InvitationConnector]

    recoverToExceptionIf[ActiveRelationshipExistsException] {
      connector.acceptInvite(acceptedInvitation)
    } map {
      _ =>
        server.findAll(postRequestedFor(urlEqualTo(acceptInviteUrl))).size() shouldBe 1
    }
  }

  it should "throw NotFoundException for a Not Found response" in {
    server.stubFor(
      post(urlEqualTo(acceptInviteUrl))
        .willReturn(
          aResponse()
            .withStatus(Status.NOT_FOUND)
            .withHeader("Content-Type", "application/json")
            .withBody(invalidResponse("NOT_FOUND"))
        )
    )

    val connector = injector.instanceOf[InvitationConnector]

    recoverToExceptionIf[NotFoundException] {
      connector.acceptInvite(acceptedInvitation)
    } map {
      _ =>
        server.findAll(postRequestedFor(urlEqualTo(acceptInviteUrl))).size() shouldBe 1
    }
  }

}

object InvitationConnectorSpec {

  private val inviteUrl = "/pension-administrator/invite"
  private val acceptInviteUrl = "/pension-administrator/accept-invite"

  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  private val pstr = "test-pstr"
  private val schemeName = "test-scheme-name"
  private val inviterPsaId = "test-inviter-psa-id"
  private val inviteePsaId = "test-invitee-psa-id"
  private val inviteeName = "test-invitee-name"
  private val declaration = true
  private val declarationDuties = true

  private val acceptedInvitation = AcceptedInvitation(
    pstr,
    inviteePsaId,
    inviterPsaId,
    declaration,
    declarationDuties,
    None
  )

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

  private def invalidResponse(code: String) =
    Json.stringify(
      Json.obj(
        "code" -> code,
        "reason" -> s"Reason for $code"
      )
    )

}
