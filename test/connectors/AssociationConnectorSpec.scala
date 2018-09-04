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
import play.api.http.Status
import play.api.http.Status._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.WireMockHelper

class AssociationConnectorSpec extends AsyncFlatSpec with Matchers with WireMockHelper with Checkers {

  override protected def portConfigKey: String = "microservice.services.pension-administrator.port"

  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  val psaid = "A1234567"

  val subscriptionDetailsUrl = s"/pension-administrator/psa-subscription-details/${psaid}"


  "AssociationConnector" should "return 200" in {

    server.stubFor(
      get(urlEqualTo(subscriptionDetailsUrl))
        .willReturn(
          aResponse()
            .withStatus(Status.OK)
        )
    )

    val connector = injector.instanceOf[AssociationConnector]

    connector.getSubscriptionDetails(psaid).map {
      result =>
        result.status shouldBe OK
        server.findAll(getRequestedFor(urlEqualTo(subscriptionDetailsUrl))).size() shouldBe 1
    }

  }

}