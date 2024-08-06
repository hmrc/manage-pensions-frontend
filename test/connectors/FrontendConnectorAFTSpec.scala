/*
 * Copyright 2024 HM Revenue & Customs
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
import models.SchemeReferenceNumber
import org.scalatest.OptionValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import play.api.http.Status
import play.api.test.FakeRequest
import play.twirl.api.Html
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.partials.HeaderCarrierForPartials
import utils.WireMockHelper

class FrontendConnectorAFTSpec extends AsyncWordSpec with Matchers with WireMockHelper with OptionValues {

  override protected def portConfigKey: String = "microservice.services.aft-frontend.port"

  implicit val headerCarrier: HeaderCarrierForPartials =
    HeaderCarrierForPartials(hc = HeaderCarrier())
  private val aftPartialUrl = "/manage-pension-scheme-accounting-for-tax/AB123456C/psa-scheme-dashboard-aft-cards"
  private val finInfoPartialUrl = "/manage-pension-scheme-accounting-for-tax/AB123456C/psa-scheme-dashboard-fin-info-cards"
  private val pspSchemeDashboardCardsUrl = "/manage-pension-scheme-accounting-for-tax/psp-scheme-dashboard-cards"
  private val paymentsAndChargesPartialHtmlUrl = "/manage-pension-scheme-accounting-for-tax/AB123456C/payments-and-charges-partial"
  private val retrievePenaltiesUrlPartialHtmlUrl = "/manage-pension-scheme-accounting-for-tax/penalties-partial"

  val srn: SchemeReferenceNumber = SchemeReferenceNumber("AB123456C")
  implicit val request: FakeRequest[_] = FakeRequest("", "")
  private val aftHtml: Html = Html("test-aft-html")
  private val finInfoHtml: Html = Html("test-fininfo-html")
  private val pspSchemeDashboardCardsHtml: Html = Html("test-psp-scheme-dashboard-cards-html")
  private val paymentsAndChargesHtml: Html = Html("test-payments-and-charges-html")
  private val retrievePenaltiesUrlPartialHtml: Html = Html("test-penalties-partial-html")


  "FrontendConnector for AFT" when {
    "asked to retrieve AFT models" should {
      "call the micro service with the correct uri and return the contents for aft" in {
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

      "call the micro service with the correct uri and return the contents for fin info" in {
        server.stubFor(
          get(urlEqualTo(finInfoPartialUrl))
            .willReturn(
              aResponse()
                .withStatus(Status.OK)
                .withHeader("Content-Type", "application/json")
                .withBody(finInfoHtml.toString())
            )
        )

        val connector = injector.instanceOf[FrontendConnector]

        connector.retrieveFinInfoPartial(srn).map(aftModel =>
          aftModel mustBe finInfoHtml
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

        connector.retrievePaymentsAndChargesPartial(srn).map(html =>
          html mustBe paymentsAndChargesHtml
        )
      }
    }
  }

  "asked to retrieve Penalties Url Partial" should {
    "call the micro service with the correct uri and return the contents" in {
      server.stubFor(
        get(urlEqualTo(retrievePenaltiesUrlPartialHtmlUrl))
          .willReturn(
            aResponse()
              .withStatus(Status.OK)
              .withHeader("Content-Type", "application/json")
              .withBody(retrievePenaltiesUrlPartialHtml.toString())
          )
      )

      val connector = injector.instanceOf[FrontendConnector]

      connector.retrievePenaltiesUrlPartial.map(penaltiesHtml =>
        penaltiesHtml mustBe retrievePenaltiesUrlPartialHtml
      )
    }
  }

  "asked to retrieve Psp Scheme Dashboard Cards" should {
    "call the micro service with the correct uri and return the contents" in {
      server.stubFor(
        get(urlEqualTo(pspSchemeDashboardCardsUrl))
          .willReturn(
            aResponse()
              .withStatus(Status.OK)
              .withHeader("Content-Type", "application/json")
              .withBody(pspSchemeDashboardCardsHtml.toString())
          )
      )

      val connector = injector.instanceOf[FrontendConnector]

      connector.retrievePspSchemeDashboardCards(srn, pspId = "psp", authorisingPsaId = "authorisingPsa").map(pspSchemeDashboard =>
        pspSchemeDashboard mustBe pspSchemeDashboardCardsHtml
      )
    }
  }

}
