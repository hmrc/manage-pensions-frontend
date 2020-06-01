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

package connectors.aft

import akka.http.scaladsl.model.HttpHeader.ParsingResult.Ok
import com.github.tomakehurst.wiremock.client.WireMock._
import identifiers.TypedIdentifier
import org.scalatest.{AsyncWordSpec, MustMatchers, OptionValues}
import play.api.http.Status
import play.api.libs.json.Json
import testhelpers.InvitationBuilder._
import uk.gov.hmrc.http.{HeaderCarrier, HttpException}
import utils.WireMockHelper
import play.api.mvc.Results


import scala.concurrent.ExecutionContext.Implicits.global

class AFTCacheConnectorSpec extends AsyncWordSpec with MustMatchers with WireMockHelper with OptionValues {

  override protected def portConfigKey: String = "microservice.services.pension-scheme-accounting-for-tax.port"

  protected implicit val hc: HeaderCarrier = HeaderCarrier()
  private val srn = "test-srn"
  private val startDate = "2020-01-01"

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
