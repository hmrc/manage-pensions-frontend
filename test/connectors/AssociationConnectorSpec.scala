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
import org.scalatest.prop.Checkers
import org.scalatest.{Matchers, AsyncFlatSpec}
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpResponse}
import utils.WireMockHelper

class AssociationConnectorSpec extends AsyncFlatSpec with Matchers with WireMockHelper with Checkers {

  override protected def portConfigKey: String = "microservice.services.pension-administrator.port"

  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  val psaId = "A1234567"

  val subscriptionDetailsUrl = s"/pension-administrator/psa-subscription-details"

  val psaIdJson = Json.stringify(
    Json.obj(
      "psaId" -> s"$psaId"
    )
  )


  "calling getSubscriptionDetails" should "return 200" in {

    server.stubFor(
      get(urlEqualTo(subscriptionDetailsUrl)).withHeader("psaId", equalTo(psaId))
        .willReturn(
          aResponse()
            .withStatus(OK)
        )
    )

    val connector = injector.instanceOf[AssociationConnector]

    connector.getSubscriptionDetails(psaId).map {
      result =>
        result.status shouldBe OK
        server.findAll(getRequestedFor(urlEqualTo(subscriptionDetailsUrl))
          .withHeader("psaId", equalTo(psaId))).size() shouldBe 1
    }

  }

  it should "throw badrequest if INVALID_PSAID" in {
    server.stubFor(
      get(urlEqualTo(subscriptionDetailsUrl)).withHeader("psaId", equalTo(psaId))
        .willReturn(
          aResponse()
            .withStatus(400).withBody("INVALID_PSAID")
        )
    )

    val connector = injector.instanceOf[AssociationConnector]


    recoverToExceptionIf[PsaIdInvalidException] {
      connector.getSubscriptionDetails(psaId)
    } map {
      _ =>
        server.findAll(getRequestedFor(urlEqualTo(subscriptionDetailsUrl))
          .withHeader("psaId", equalTo(psaId))).size() shouldBe 1
    }
  }

  it should "throw badrequest if INVALID_CORRELATIONID" in {
    server.stubFor(
      get(urlEqualTo(subscriptionDetailsUrl)).withHeader("psaId", equalTo(psaId))
        .willReturn(
          aResponse()
            .withStatus(400).withBody("INVALID_CORRELATIONID")
        )
    )

    val connector = injector.instanceOf[AssociationConnector]


    recoverToExceptionIf[CorrelationIdInvalidException] {
      connector.getSubscriptionDetails(psaId)
    } map {
      _ =>
        server.findAll(getRequestedFor(urlEqualTo(subscriptionDetailsUrl))
          .withHeader("psaId", equalTo(psaId))).size() shouldBe 1
    }
  }
  it should "throw Not Found" in {
    server.stubFor(
      get(urlEqualTo(subscriptionDetailsUrl)).withHeader("psaId", equalTo(psaId))
        .willReturn(
          notFound()
        )
    )

    val connector = injector.instanceOf[AssociationConnector]


    recoverToExceptionIf[PsaIdNotFoundException] {
      connector.getSubscriptionDetails(psaId)
    } map {
      _ =>
        server.findAll(getRequestedFor(urlEqualTo(subscriptionDetailsUrl))
          .withHeader("psaId", equalTo(psaId))).size() shouldBe 1
    }
  }

  it should "throw Generic exception for all others" in {
    server.stubFor(
      get(urlEqualTo(subscriptionDetailsUrl)).withHeader("psaId", equalTo(psaId))
        .willReturn(
          serverError()
        )
    )

    val connector = injector.instanceOf[AssociationConnector]


    recoverToExceptionIf[Exception] {
      connector.getSubscriptionDetails(psaId)
    } map {
      _ =>
        server.findAll(getRequestedFor(urlEqualTo(subscriptionDetailsUrl))
          .withHeader("psaId", equalTo(psaId))).size() shouldBe 1
    }
  }

}