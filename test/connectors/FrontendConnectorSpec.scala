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

class FrontendConnectorSpec extends AsyncWordSpec with Matchers with WireMockHelper with OptionValues {

  override protected def portConfigKey: String = "microservice.services.aft-frontend.port"

  implicit val headerCarrier: HeaderCarrierForPartials =
    HeaderCarrierForPartials(hc = HeaderCarrier())
  private val aftPartialUrl = "/manage-pension-scheme-accounting-for-tax/srn/psa-scheme-dashboard-cards"
  private val erPartialUrl = "/manage-pension-scheme-event-report/event-reporting-partials"
  private val pspSchemeDashboardCardsUrl = "/manage-pension-scheme-accounting-for-tax/psp-scheme-dashboard-cards"
  private val paymentsAndChargesPartialHtmlUrl = "/manage-pension-scheme-accounting-for-tax/srn/payments-and-charges-partial"
  private val schemeUrlsPartialHtmlUrl = "/register-pension-scheme/urls-partial"
  private val retrievePenaltiesUrlPartialHtmlUrl = "/manage-pension-scheme-accounting-for-tax/penalties-partial"
  private val migrationUrlsHtmlUrl = "/add-pension-scheme/migration-tile"
  private val srn = "srn"
  implicit val request: FakeRequest[_] = FakeRequest("", "")
  private val aftHtml: Html = Html("test-aft-html")
  private val erHtml: Html = Html("test-er-html")
  private val pspSchemeDashboardCardsHtml: Html = Html("test-psp-scheme-dashboard-cards-html")
  private val paymentsAndChargesHtml: Html = Html("test-payments-and-charges-html")
  private val schemeUrlsPartialHtml: Html = Html("test-scheme-partial-html")
  private val retrievePenaltiesUrlPartialHtml: Html = Html("test-penalties-partial-html")
  private val migrationUrlsHtml: Html = Html("test-migration-partial-html")

  "FrontendConnector" when {
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

    "asked to retrieve Event Reporting models" should {
      "call the micro service with the correct uri and return the contents" in {
        server.stubFor(
          get(urlEqualTo(erPartialUrl))
            .willReturn(
              aResponse()
                .withStatus(Status.OK)
                .withHeader("Content-Type", "application/json")
                .withBody(erHtml.toString())
            )
        )

        val connector = injector.instanceOf[FrontendConnector]

        connector.retrieveEventReportingPartial.map(erModel =>
          erModel mustBe erHtml
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

      connector.retrieveSchemeUrlsPartial.map(schemeUrlsPartialHtml =>
        schemeUrlsPartialHtml mustBe schemeUrlsPartialHtml
      )
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

  "asked to retrieve Migration Urls Partial" should {
    "call the micro service with the correct uri and return the contents" in {
      server.stubFor(
        get(urlEqualTo(migrationUrlsHtmlUrl))
          .willReturn(
            aResponse()
              .withStatus(Status.OK)
              .withHeader("Content-Type", "application/json")
              .withBody(migrationUrlsHtml.toString())
          )
      )

      val connector = injector.instanceOf[FrontendConnector]

      connector.retrieveMigrationUrlsPartial.map(migrationUrlsHtml =>
        migrationUrlsHtml mustBe migrationUrlsHtml
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
