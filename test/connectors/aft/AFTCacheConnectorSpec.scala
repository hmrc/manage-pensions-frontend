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

package connectors.aft

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.OptionValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import play.api.mvc.Results
import uk.gov.hmrc.http.HeaderCarrier
import utils.WireMockHelper

class AFTCacheConnectorSpec extends AsyncWordSpec with Matchers with WireMockHelper with OptionValues {

  override protected def portConfigKey: String = "microservice.services.pension-scheme-accounting-for-tax.port"

  protected implicit val hc: HeaderCarrier = HeaderCarrier()

  protected val lockUrl: String = "/pension-scheme-accounting-for-tax/journey-cache/aft/lock"

  protected lazy val connector: AftCacheConnector = injector.instanceOf[AftCacheConnector]

  ".removeLock" must {

    "return OK after removing all the data from the collection" in {
      server.stubFor(delete(urlEqualTo(lockUrl)).
        willReturn(ok)
      )
      connector.removeLock.map {
        _ mustEqual Results.Ok
      }
    }
  }
}
