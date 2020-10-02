/*
 * Copyright 2020 HM Revenue & Customs
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
import org.scalatest.AsyncFlatSpec
import org.scalatest.Matchers
import org.scalatestplus.scalacheck.Checkers
import play.api.http.Status._
import play.api.libs.json.JsResultException
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.UpstreamErrorResponse
import utils.WireMockHelper

class SubscriptionConnectorSpec extends AsyncFlatSpec with Matchers with WireMockHelper with Checkers {

  override protected def portConfigKey: String = "microservice.services.pension-administrator.port"

  lazy val connector = injector.instanceOf[SubscriptionConnector]

  import SubscriptionConnectorSpec._

  "calling getSubscriptionDetails" should "return 200" in {

    server.stubFor(
      get(urlEqualTo(subscriptionDetailsUrl)).withHeader("psaId", equalTo(psaId))
        .willReturn(
          aResponse()
            .withStatus(OK).withBody(successResponse)
        )
    )

    connector.getSubscriptionDetails(psaId).map {
      result =>
        Json.toJson(result) shouldBe Json.parse(successResponse)
        server.findAll(getRequestedFor(urlEqualTo(subscriptionDetailsUrl))
          .withHeader("psaId", equalTo(psaId))).size() shouldBe 1
    }

  }

  it should "throw exception if failed to parse the json" in {

    server.stubFor(
      get(urlEqualTo(subscriptionDetailsUrl)).withHeader("psaId", equalTo(psaId))
        .willReturn(
          aResponse()
            .withStatus(OK).withBody(invalidResponse)
        )
    )

    recoverToExceptionIf[JsResultException] {
      connector.getSubscriptionDetails(psaId)
    } map {
      _ =>
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

    recoverToExceptionIf[PsaIdInvalidSubscriptionException] {
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

    recoverToExceptionIf[CorrelationIdInvalidSubscriptionException] {
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

    recoverToExceptionIf[PsaIdNotFoundSubscriptionException] {
      connector.getSubscriptionDetails(psaId)
    } map {
      _ =>
        server.findAll(getRequestedFor(urlEqualTo(subscriptionDetailsUrl))
          .withHeader("psaId", equalTo(psaId))).size() shouldBe 1
    }
  }

  it should "throw Upstream5xxResponse for internal server error" in {
    server.stubFor(
      get(urlEqualTo(subscriptionDetailsUrl)).withHeader("psaId", equalTo(psaId))
        .willReturn(
          serverError()
        )
    )

    recoverToExceptionIf[UpstreamErrorResponse] {
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

    recoverToExceptionIf[Exception] {
      connector.getSubscriptionDetails(psaId)
    } map {
      _ =>
        server.findAll(getRequestedFor(urlEqualTo(subscriptionDetailsUrl))
          .withHeader("psaId", equalTo(psaId))).size() shouldBe 1
    }
  }

}

object SubscriptionConnectorSpec extends JsonFileReader {

  implicit val headerCarrier: HeaderCarrier = HeaderCarrier()
  val psaId = "A1234567"
  val subscriptionDetailsUrl = s"/pension-administrator/psa-subscription-details"

  val psaIdJson = Json.stringify(
    Json.obj(
      "psaId" -> s"$psaId"
    )
  )

  val successResponse = readJsonFromFile("/data/validSubscription.json").toString()

  val invalidResponse = """{"invalid" : "response"}"""

}
