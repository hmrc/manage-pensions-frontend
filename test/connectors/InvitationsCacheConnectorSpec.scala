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
import identifiers.TypedIdentifier
import models.Invitation
import org.joda.time.DateTime
import org.scalatest.{AsyncWordSpec, MustMatchers, OptionValues}
import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, HttpException}
import utils.WireMockHelper

import scala.concurrent.ExecutionContext.Implicits.global

class InvitationsCacheConnectorSpec extends AsyncWordSpec with MustMatchers with WireMockHelper with OptionValues {

  import InvitationsCacheConnectorSpec._

  protected object FakeIdentifier extends TypedIdentifier[String] {
    override def toString: String = "fake-identifier"
  }

  override protected def portConfigKey: String = "microservice.services.pension-administrator.port"

  protected implicit val hc: HeaderCarrier = HeaderCarrier()

  protected val addUrl: String = "/pension-administrator/invitation/add"
  protected val getUrl = "/pension-administrator/invitation/get"
  protected val getForSchemeUrl = "/pension-administrator/invitation/get-for-scheme"
  protected val getForInviteeUrl = "/pension-administrator/invitation/get-for-invitee"
  protected val removeUrl = "/pension-administrator/invitation"

  protected lazy val connector: InvitationsCacheConnector = injector.instanceOf[InvitationsCacheConnector]


  "get" must {

    "return `None` when the server returns a 404" in {

      server.stubFor(
        get(urlEqualTo(getUrl))
          .willReturn(
            notFound
          )
      )

      connector.get(pstr1, inviteePsaId1) map {
        result =>
          result mustEqual List.empty
      }
    }

    "return data when the server returns 200" in {
      server.stubFor(
        get(urlEqualTo(getUrl))
          .willReturn(
            ok(Json.toJson(invitationList).toString)
          )
      )

      connector.get(pstr1, inviteePsaId1) map {
        result =>
          result mustEqual invitationList
      }
    }

    "return a failed future on upstream error" in {

      server.stubFor(
        get(urlEqualTo(getUrl))
          .willReturn(
            serverError
          )
      )

      recoverToExceptionIf[HttpException] {
        connector.get(pstr1, inviteePsaId1)
      } map {
        _.responseCode mustEqual Status.INTERNAL_SERVER_ERROR
      }

    }
  }

  "getForScheme" must {

    "return `None` when the server returns a 404" in {

      server.stubFor(
        get(urlEqualTo(getForSchemeUrl))
          .willReturn(
            notFound
          )
      )

      connector.getForScheme(pstr1) map {
        result =>
          result mustEqual List.empty
      }
    }

    "return data when the server returns 200" in {
      server.stubFor(
        get(urlEqualTo(getForSchemeUrl))
          .willReturn(
            ok(Json.toJson(invitationList).toString)
          )
      )

      connector.getForScheme(pstr1) map {
        result =>
          result mustEqual invitationList
      }
    }

    "return a failed future on upstream error" in {

      server.stubFor(
        get(urlEqualTo(getForSchemeUrl))
          .willReturn(
            serverError
          )
      )

      recoverToExceptionIf[HttpException] {
        connector.getForScheme(pstr1)
      } map {
        _.responseCode mustEqual Status.INTERNAL_SERVER_ERROR
      }

    }
  }

  "getForInvitee" must {

    "return `None` when the server returns a 404" in {

      server.stubFor(
        get(urlEqualTo(getForInviteeUrl))
          .willReturn(
            notFound
          )
      )

      connector.getForInvitee(inviteePsaId1) map {
        result =>
          result mustEqual List.empty
      }
    }

    "return data when the server returns 200" in {
      server.stubFor(
        get(urlEqualTo(getForInviteeUrl))
          .willReturn(
            ok(Json.toJson(invitationList).toString)
          )
      )

      connector.getForInvitee(inviteePsaId1) map {
        result =>
          result mustEqual invitationList
      }
    }

    "return a failed future on upstream error" in {

      server.stubFor(
        get(urlEqualTo(getForInviteeUrl))
          .willReturn(
            serverError
          )
      )

      recoverToExceptionIf[HttpException] {
        connector.getForInvitee(inviteePsaId1)
      } map {
        _.responseCode mustEqual Status.INTERNAL_SERVER_ERROR
      }

    }
  }

  "add" must {
    "return OK when valid invitation json is posted" in {
      server.stubFor(
        post(urlEqualTo(addUrl))
          .withRequestBody(equalTo(Json.toJson(invitation1).toString()))
          .willReturn(ok)
      )
      connector.add(invitation1) map {
        _ mustEqual (())
      }
    }


    "return a failed future on upstream error" in {

      server.stubFor(
        post(urlEqualTo(addUrl))
          .withRequestBody(equalTo(Json.toJson(invitation1).toString()))
          .willReturn(serverError)
      )

      recoverToExceptionIf[HttpException] {
        connector.add(invitation1)
      } map {
        _.responseCode mustEqual Status.INTERNAL_SERVER_ERROR
      }
    }

    "remove" must {
      "remove existing data" in {

        server.stubFor(
          delete(urlEqualTo(removeUrl))
            .willReturn(
              ok
            )
        )

        connector.remove(pstr1, inviteePsaId1) map {
          _ mustEqual (())
        }
      }
    }
  }
}

object InvitationsCacheConnectorSpec {

  private val pstr1 = "S12345"
  private val schemeName1 = "Test scheme1 name"
  private val inviterPsaId1 = "I12345"
  private val inviteePsaId1 = "P12345"
  private val inviteeName1 = "Test Invitee1 Name"
  private val expiryDate1 = new DateTime("2018-11-10")

  private val pstr2 = "D1234"
  private val schemeName2 = "Test scheme2 name"
  private val inviterPsaId2 = "Q12345"
  private val inviteePsaId2 = "T12345"
  private val inviteeName2 = "Test Invitee2 Name"
  private val expiryDate2 = new DateTime("2018-11-11")

  private val invitation1 =
    Invitation(pstr = pstr1, schemeName = schemeName1, inviterPsaId = inviterPsaId1, inviteePsaId = inviteePsaId1, inviteeName = inviteeName1, expireAt = expiryDate1)
  private val invitation2 =
    Invitation(pstr = pstr2, schemeName = schemeName2, inviterPsaId = inviterPsaId2, inviteePsaId = inviteePsaId2, inviteeName = inviteeName2, expireAt = expiryDate2)
  private val invitationList = List(invitation1, invitation2)
}
