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
import com.google.inject.{ImplementedBy, Inject}
import config.FrontendAppConfig
import connectors.InvitationConnectorSpec.{invitation, inviteUrl, requestJson}
import models.PsaToBeRemovedFromScheme
import org.joda.time.LocalDate
import org.scalatest.{AsyncFlatSpec, Matchers}
import org.scalatest.prop.Checkers
import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import utils.{HttpResponseHelper, WireMockHelper}

import scala.concurrent.{ExecutionContext, Future}

class PsaRemovalConnectorSpec extends AsyncFlatSpec with Matchers with WireMockHelper with Checkers {

  import PsaRemovalConnectorSpec._
  override protected def portConfigKey: String = "microservice.services.pension-administrator.port"

  "Delete" should "return successful following a successful deletion" in {
    server.stubFor(
      post(urlEqualTo(deleteUrl))
        .withRequestBody(equalToJson(requestJson))
        .willReturn(
          aResponse()
            .withStatus(Status.OK)
        )
    )

    val connector = injector.instanceOf[PsaRemovalConnector]

    connector.remove(psaToBeRemoved).map {
      _ => server.findAll(postRequestedFor(urlEqualTo(deleteUrl))).size() shouldBe 1
    }
  }
}

object PsaRemovalConnectorSpec {
  implicit val hc : HeaderCarrier = HeaderCarrier()

  private val psaToBeRemoved = PsaToBeRemovedFromScheme("238DAJFAS", "XXAJ329AJJ", new LocalDate(2009,1,1))
  private val deleteUrl = "/pension-administrator/delete"
  private val requestJson = Json.stringify(Json.toJson(psaToBeRemoved))
}
