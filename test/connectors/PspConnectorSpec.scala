/*
 * Copyright 2021 HM Revenue & Customs
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
import models.DeAuthorise
import org.scalatest.{AsyncFlatSpec, Matchers}
import org.scalatestplus.scalacheck.Checkers
import play.api.http.Status._
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, UpstreamErrorResponse}
import utils.WireMockHelper

class PspConnectorSpec extends AsyncFlatSpec with Matchers with WireMockHelper with Checkers {
  override protected def portConfigKey: String = "microservice.services.pension-practitioner.port"

  import connectors.PspConnectorSpec._

  "authorisePsp" should "return successfully for PSA authorising PSP" in {
    server.stubFor(
      post(urlEqualTo(pspAuthUrl))
        .withRequestBody(equalToJson(Json.stringify(Json.toJson(pspAuthJson))))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withBody(Json.stringify(Json.obj("processingDate" -> LocalDateTime.now())))
        )
    )

    val connector = injector.instanceOf[PspConnector]

    connector.authorisePsp(pstr, psaId, pspId, Some(cr)) map {
      response =>
        server.findAll(postRequestedFor(urlEqualTo(pspAuthUrl))).size() shouldBe 1
    }
  }

  "authorisePsp" should "fail if ACTIVE_RELATIONSHIP_EXISTS" in {
    server.stubFor(
      post(urlEqualTo(pspAuthUrl))
        .withRequestBody(equalToJson(Json.stringify(Json.toJson(pspAuthJson))))
        .willReturn(
          aResponse()
            .withStatus(FORBIDDEN)
            .withBody(Json.stringify(activeRelationshipJson))
        )
    )

    val connector = injector.instanceOf[PspConnector]

    recoverToSucceededIf[ActiveRelationshipExistsException] {
      connector.authorisePsp(pstr, psaId, pspId, Some(cr))
    } map {
      _ =>
        server.findAll(postRequestedFor(urlEqualTo(pspAuthUrl))).size() shouldBe 1
    }
  }

  "authorisePsp" should "fail if bad request" in {
    server.stubFor(
      post(urlEqualTo(pspAuthUrl))
        .withRequestBody(equalToJson(Json.stringify(Json.toJson(pspAuthJson))))
        .willReturn(
          aResponse()
            .withStatus(BAD_REQUEST)
        )
    )

    val connector = injector.instanceOf[PspConnector]

    recoverToSucceededIf[BadRequestException] {
      connector.authorisePsp(pstr, psaId, pspId, Some(cr))
    } map {
      _ =>
        server.findAll(postRequestedFor(urlEqualTo(pspAuthUrl))).size() shouldBe 1
    }
  }

  "authorisePsp" should "fail if UpstreamErrorResponse" in {
    server.stubFor(
      post(urlEqualTo(pspAuthUrl))
        .withRequestBody(equalToJson(Json.stringify(Json.toJson(pspAuthJson))))
        .willReturn(
          aResponse()
            .withStatus(INTERNAL_SERVER_ERROR)
        )
    )

    val connector = injector.instanceOf[PspConnector]

    recoverToSucceededIf[UpstreamErrorResponse] {
      connector.authorisePsp(pstr, psaId, pspId, Some(cr))
    } map {
      _ =>
        server.findAll(postRequestedFor(urlEqualTo(pspAuthUrl))).size() shouldBe 1
    }
  }

  "deAuthorise" should "return successfully for PSA deAuth PSA" in {
    server.stubFor(
      post(urlEqualTo(deAuthUrl))
        .withRequestBody(equalToJson(Json.stringify(Json.toJson(psaDeAuthPsaJson))))
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
        .withRequestBody(equalToJson(Json.stringify(psaDeAuthPspJson)))
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
        .withRequestBody(equalToJson(Json.stringify(pspDeAuthPspJson)))
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
            .withBody(Json.stringify(duplicateSubmissionJson))
        )
    )

    val connector = injector.instanceOf[PspConnector]

    recoverToSucceededIf[DuplicateSubmissionException] {
      connector.deAuthorise(pstr, psaDeAuthPsa)
    } map {
      _ =>
        server.findAll(postRequestedFor(urlEqualTo(deAuthUrl))).size() shouldBe 1
    }
  }

  "deAuthorise" should "fail if bad request" in {
    server.stubFor(
      post(urlEqualTo(deAuthUrl))
        .withRequestBody(equalToJson(Json.stringify(Json.toJson(psaDeAuthPsa))))
        .willReturn(
          aResponse()
            .withStatus(BAD_REQUEST)
        )
    )

    val connector = injector.instanceOf[PspConnector]

    recoverToSucceededIf[BadRequestException] {
      connector.deAuthorise(pstr, psaDeAuthPsa)
    } map {
      _ =>
        server.findAll(postRequestedFor(urlEqualTo(deAuthUrl))).size() shouldBe 1
    }
  }

  "deAuthorise" should "fail if UpstreamErrorResponse" in {
    server.stubFor(
      post(urlEqualTo(deAuthUrl))
        .withRequestBody(equalToJson(Json.stringify(Json.toJson(psaDeAuthPsa))))
        .willReturn(
          aResponse()
            .withStatus(INTERNAL_SERVER_ERROR)
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
  private val pspAuthUrl = "/pension-practitioner/authorise-psp"

  private val pstr = "0"

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  val psaDeAuthPsa: DeAuthorise = DeAuthorise(
    ceaseIDType = "PSAID",
    ceaseNumber = "A1234567",
    initiatedIDType = "PSAID",
    initiatedIDNumber = "A2000567",
    ceaseDate = "2019-03-29"
  )

  val psaDeAuthPsp: DeAuthorise = DeAuthorise(
    ceaseIDType = "PSPID",
    ceaseNumber = "21234568",
    initiatedIDType = "PSAID",
    initiatedIDNumber = "A1234568",
    ceaseDate = "2019-03-29"
  )

  val pspDeAuthPsp: DeAuthorise = DeAuthorise(
    ceaseIDType = "PSPID",
    ceaseNumber = "21234568",
    initiatedIDType = "PSPID",
    initiatedIDNumber = "21234569",
    ceaseDate = "2019-03-29"
  )

  val psaDeAuthPsaJson: JsObject = Json.obj(
    "ceaseIDType" -> "PSAID",
    "ceaseNumber" -> "A1234567",
    "initiatedIDType" -> "PSAID",
    "initiatedIDNumber" -> "A2000567",
    "ceaseDate" -> "2019-03-29"
  )

  val psaDeAuthPspJson: JsObject = Json.obj(
    "ceaseIDType" -> "PSPID",
    "ceaseNumber" -> "21234568",
    "initiatedIDType" -> "PSAID",
    "initiatedIDNumber" -> "A1234568",
    "ceaseDate" -> "2019-03-29",
    "declarationCeasePSPDetails" ->
      Json.obj("declarationBox1" -> true)
  )

  val pspDeAuthPspJson: JsObject = Json.obj(
    "ceaseIDType" -> "PSPID",
    "ceaseNumber" -> "21234568",
    "initiatedIDType" -> "PSPID",
    "initiatedIDNumber" -> "21234569",
    "ceaseDate" -> "2019-03-29",
    "declarationCeasePSPDetails" ->
      Json.obj("declarationBox2" -> true)
  )

  private val duplicateSubmissionJson = Json.obj(
    "code" -> "DUPLICATE_SUBMISSION",
    "reason" -> "The remote endpoint has indicated that duplicate submission."
  )

  private val activeRelationshipJson = Json.obj(
    "code" -> "ACTIVE_RELATIONSHIP_EXISTS",
    "reason" -> "The remote endpoint has indicated that an active relation already exists"
  )

  val psaId: String = "A0000000"
  val pspId: String = "00000000"
  val cr: String = "xyz"
  val pspAuthJson: JsObject = Json.obj(
    "pspAssociationIDsDetails" -> Json.obj(
      "inviteeIDType" -> "PSPID",
      "inviterPSAID" -> psaId,
      "inviteeIDNumber" -> pspId,
      "clientReference" -> cr),
    "pspDeclarationDetails" -> Json.obj("box1" -> true, "box2" -> true, "box3" -> true)
  )
}
