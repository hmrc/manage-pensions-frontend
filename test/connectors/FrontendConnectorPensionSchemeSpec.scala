/*
 * Copyright 2023 HM Revenue & Customs
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
import org.scalatest.OptionValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import play.api.http.Status
import play.api.test.FakeRequest
import play.twirl.api.Html
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.partials.HeaderCarrierForPartials
import utils.WireMockHelper

class FrontendConnectorPensionSchemeSpec extends AsyncWordSpec with Matchers with WireMockHelper with OptionValues {

  override protected def portConfigKey: String = "microservice.services.scheme-frontend.port"

  implicit val headerCarrier: HeaderCarrierForPartials =
    HeaderCarrierForPartials(hc = HeaderCarrier())
  private val schemeUrlsPartialHtmlUrl = "/register-pension-scheme/urls-partial"
  implicit val request: FakeRequest[_] = FakeRequest("", "")
  private val schemeUrlsPartialHtml: Html = Html("test-scheme-partial-html")

  "FrontendConnector for Pension Scheme" when {
    "asked to retrieve Scheme Urls Partial" should {
      "call the micro service with the correct uri and return the contents" in {
        server.stubFor(
          get(urlEqualTo(schemeUrlsPartialHtmlUrl))
            .willReturn(
              aResponse()
                .withStatus(Status.OK)
                .withHeader("Content-Type", "application/json")
                .withBody(schemeUrlsPartialHtml.toString())
            )
        )

        val connector = injector.instanceOf[FrontendConnector]

        connector.retrieveSchemeUrlsPartial.map(html =>
          html mustBe schemeUrlsPartialHtml
        )
      }
    }
  }

}
