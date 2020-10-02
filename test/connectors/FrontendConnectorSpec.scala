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

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.AsyncWordSpec
import org.scalatest.MustMatchers
import org.scalatest.OptionValues
import play.api.http.Status
import play.api.test.FakeRequest
import play.twirl.api.Html
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.partials.HeaderCarrierForPartials
import utils.WireMockHelper

class FrontendConnectorSpec extends AsyncWordSpec with MustMatchers with WireMockHelper with OptionValues {

  override protected def portConfigKey: String = "microservice.services.aft-frontend.port"

  implicit val headerCarrier: HeaderCarrierForPartials =
    HeaderCarrierForPartials(hc = HeaderCarrier(), encodedCookies = "")
  private val aftPartialUrl = "/manage-pension-scheme-accounting-for-tax/srn/aft-partial"
  private val paymentsAndChargesPartialHtmlUrl = "/manage-pension-scheme-accounting-for-tax/srn/payments-and-charges-partial"
  private val srn = "srn"
  implicit val request: FakeRequest[_] = FakeRequest("", "")
  private val aftHtml: Html = Html("test-aft-html")
  private val paymentsAndChargesHtml: Html = Html("test-payments-and-charges-html")

  "FrontedConnector" when {
    "asked to retrieve AFT models" should {
      "call the micro service with the correct uri and return the contents" in {
        server.stubFor(
          get(urlEqualTo(aftPartialUrl))
            .willReturn(
              aResponse()
                .withStatus(Status.OK)
                .withHeader("Content-Type", "application/json")
                .withBody(aftHtml.toString())
            )
        )

        val connector = injector.instanceOf[FrontendConnector]

        connector.retrieveAftPartial(srn).map(aftModel =>
          aftModel mustBe aftHtml
        )
      }
    }

    "asked to retrieve Payment and charges partial" should {
      "call the micro service with the correct uri and return the contents" in {
        server.stubFor(
          get(urlEqualTo(paymentsAndChargesPartialHtmlUrl))
            .willReturn(
              aResponse()
                .withStatus(Status.OK)
                .withHeader("Content-Type", "application/json")
                .withBody(paymentsAndChargesHtml.toString())
            )
        )

        val connector = injector.instanceOf[FrontendConnector]

        connector.retrievePaymentsAndChargesPartial(srn).map(paymentsAndChargesHtml =>
          paymentsAndChargesHtml mustBe paymentsAndChargesHtml
        )
      }
    }
  }

}
