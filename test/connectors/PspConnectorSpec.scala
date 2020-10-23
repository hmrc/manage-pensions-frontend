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

package connectors

import java.time.LocalDateTime

import com.github.tomakehurst.wiremock.client.WireMock._
import models.invitations.psp.DeAuthorise
import org.scalatest.{AsyncFlatSpec, Matchers}
import org.scalatestplus.scalacheck.Checkers
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import utils.WireMockHelper

class PspConnectorSpec extends AsyncFlatSpec with Matchers with WireMockHelper with Checkers {
  override protected def portConfigKey: String = "microservice.services.pension-practitioner.port"

  import connectors.PspConnectorSpec._

  "deAuthorise" should "return successfully for PSA deAuth PSA" in {
    server.stubFor(
      post(urlEqualTo(deAuthUrl))
        .withRequestBody(equalToJson(Json.stringify(Json.toJson(psaDeAuthPsa))))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withBody(Json.stringify(Json.obj("processingDate" -> LocalDateTime.now())))
        )
    )

    val connector = injector.instanceOf[PspConnector]

    connector.deAuthorise(pstr, psaDeAuthPsa) map {
      response =>
        server.findAll(postRequestedFor(urlEqualTo(deAuthUrl))).size() shouldBe 1
        response.status shouldBe 200
        (Json.parse(response.body) \ "processingDate").asOpt[String].isDefined shouldBe true
    }
  }

  "deAuthorise" should "return successfully for PSA deAuth PSP" in {
    server.stubFor(
      post(urlEqualTo(deAuthUrl))
        .withRequestBody(equalToJson(Json.stringify(Json.toJson(psaDeAuthPsp))))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withBody(Json.stringify(Json.obj("processingDate" -> LocalDateTime.now())))
        )
    )

    val connector = injector.instanceOf[PspConnector]

    connector.deAuthorise(pstr, psaDeAuthPsp) map {
      response =>
        server.findAll(postRequestedFor(urlEqualTo(deAuthUrl))).size() shouldBe 1
        response.status shouldBe 200
        (Json.parse(response.body) \ "processingDate").asOpt[String].isDefined shouldBe true
    }
  }

  "deAuthorise" should "return successfully for PSP deAuth PSP" in {
    server.stubFor(
      post(urlEqualTo(deAuthUrl))
        .withRequestBody(equalToJson(Json.stringify(Json.toJson(pspDeAuthPsp))))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withBody(Json.stringify(Json.obj("processingDate" -> LocalDateTime.now())))
        )
    )

    val connector = injector.instanceOf[PspConnector]

    connector.deAuthorise(pstr, pspDeAuthPsp) map {
      response =>
        server.findAll(postRequestedFor(urlEqualTo(deAuthUrl))).size() shouldBe 1
        response.status shouldBe 200
        (Json.parse(response.body) \ "processingDate").asOpt[String].isDefined shouldBe true
    }
  }

  "deAuthorise" should "fail if duplicate request made" in {
    server.stubFor(
      post(urlEqualTo(deAuthUrl))
        .withRequestBody(equalToJson(Json.stringify(Json.toJson(psaDeAuthPsa))))
        .willReturn(
          aResponse()
            .withStatus(CONFLICT)
        )
    )

    val connector = injector.instanceOf[PspConnector]

    recoverToSucceededIf[UpstreamErrorResponse] {
      connector.deAuthorise(pstr, psaDeAuthPsa)
    } map {
      _ =>
        server.findAll(postRequestedFor(urlEqualTo(deAuthUrl))).size() shouldBe 1
    }
  }
}

object PspConnectorSpec {
  private val deAuthUrl = "/pension-practitioner/de-authorise-psp"

  private val pstr = "0"

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private val psaDeAuthPsa = DeAuthorise(
    ceaseIDType = "PSAID",
    ceaseNumber = "A1234567",
    initiatedIDType = "PSAID",
    initiatedIDNumber = "A2000567",
    ceaseDate = "2019-03-29"
  )

  private val psaDeAuthPsp = DeAuthorise(
    ceaseIDType = "PSPID",
    ceaseNumber = "21234568",
    initiatedIDType = "PSAID",
    initiatedIDNumber = "A1234568",
    ceaseDate = "2019-03-29"
  )

  private val pspDeAuthPsp = DeAuthorise(
    ceaseIDType = "PSPID",
    ceaseNumber = "21234568",
    initiatedIDType = "PSPID",
    initiatedIDNumber = "21234569",
    ceaseDate = "2019-03-29"
  )
}
