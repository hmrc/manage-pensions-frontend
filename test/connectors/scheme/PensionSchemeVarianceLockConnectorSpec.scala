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

package connectors.scheme

import com.github.tomakehurst.wiremock.client.WireMock._
import models.PsaLock
import models.SchemeVariance
import models.VarianceLock
import org.scalatest.AsyncFlatSpec
import org.scalatest.Matchers
import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpException
import utils.WireMockHelper

class PensionSchemeVarianceLockConnectorSpec extends AsyncFlatSpec with Matchers with WireMockHelper {

  override protected def portConfigKey: String = "microservice.services.pensions-scheme.port"

  import PensionSchemeVarianceLockConnectorSpec._

  "isLockByPsaIdOrSchemeId" should "return the VarianceLock for a valid request/response" in {

    server.stubFor(
      get(urlEqualTo(isLockByPsaOrSchemeUrl))
        .withHeader("psaId", equalTo(psaId))
        .withHeader("srn", equalTo(srn))
        .willReturn(
          aResponse()
            .withStatus(Status.OK)
            .withHeader("Content-Type", "application/json")
            .withBody(Json.toJson(VarianceLock).toString())
        )
    )

    val connector = injector.instanceOf[PensionSchemeVarianceLockConnectorImpl]

    connector.isLockByPsaIdOrSchemeId(psaId, srn).map(schemeVariance =>
      schemeVariance shouldBe Some(VarianceLock)
    )

  }

  it should "return the Lock for a PsaLock request/response" in {

    server.stubFor(
      get(urlEqualTo(isLockByPsaOrSchemeUrl))
        .withHeader("psaId", equalTo(psaId))
        .withHeader("srn", equalTo(srn))
        .willReturn(
          aResponse()
            .withStatus(Status.OK)
            .withHeader("Content-Type", "application/json")
            .withBody(Json.toJson(PsaLock).toString())
        )
    )

    val connector = injector.instanceOf[PensionSchemeVarianceLockConnectorImpl]

    connector.isLockByPsaIdOrSchemeId(psaId, srn).map(schemeVariance =>
      schemeVariance shouldBe Some(PsaLock)
    )

  }

  it should "return a failed future on upstream error" in {

    server.stubFor(
      get(urlEqualTo(isLockByPsaOrSchemeUrl))
        .willReturn(
          serverError
        )
    )

    val connector = injector.instanceOf[PensionSchemeVarianceLockConnectorImpl]

    recoverToExceptionIf[HttpException] {
      connector.isLockByPsaIdOrSchemeId(psaId, srn)
    } map {
      _.responseCode shouldBe Status.INTERNAL_SERVER_ERROR
    }
  }

  "getLock" should "return the valid request/response" in {

    server.stubFor(
      get(urlEqualTo(getLockUrl))
        .withHeader("psaId", equalTo(psaId))
        .withHeader("srn", equalTo(srn))
        .willReturn(
          aResponse()
            .withStatus(Status.OK)
            .withHeader("Content-Type", "application/json")
            .withBody(validSchemeVarianceLockResponse)
        )
    )

    val connector = injector.instanceOf[PensionSchemeVarianceLockConnectorImpl]

    connector.getLock(psaId, srn).map(schemeVariance =>
      schemeVariance shouldBe Some(schemeVarianceLockResponse)
    )

  }

  it should "return none if no lock found" in {

    server.stubFor(
      get(urlEqualTo(getLockUrl))
        .withHeader("psaId", equalTo(psaId))
        .withHeader("srn", equalTo(srn))
        .willReturn(
          aResponse()
            .withStatus(Status.NOT_FOUND)
        )
    )

    val connector = injector.instanceOf[PensionSchemeVarianceLockConnectorImpl]

    connector.getLock(psaId, srn).map(schemeVariance =>
      schemeVariance shouldBe None
    )
  }

  it should "return a failed future on upstream error" in {

    server.stubFor(
      get(urlEqualTo(getLockUrl))
        .withHeader("psaId", equalTo(psaId))
        .withHeader("srn", equalTo(srn))
        .willReturn(
          serverError
        )
    )

    val connector = injector.instanceOf[PensionSchemeVarianceLockConnectorImpl]

    recoverToExceptionIf[HttpException] {
      connector.getLock(psaId, srn)
    } map {
      _.responseCode shouldBe Status.INTERNAL_SERVER_ERROR
    }
  }

  "getLockByPsa" should "return the valid request/response" in {

    server.stubFor(
      get(urlEqualTo(getLockByPsaUrl))
        .withHeader("psaId", equalTo(psaId))
        .willReturn(
          aResponse()
            .withStatus(Status.OK)
            .withHeader("Content-Type", "application/json")
            .withBody(validSchemeVarianceLockResponse)
        )
    )

    val connector = injector.instanceOf[PensionSchemeVarianceLockConnectorImpl]

    connector.getLockByPsa(psaId).map(schemeVariance =>
      schemeVariance shouldBe Some(schemeVarianceLockResponse)
    )

  }

  it should "return none if no lock found" in {

    server.stubFor(
      get(urlEqualTo(getLockByPsaUrl))
        .withHeader("psaId", equalTo(psaId))
        .willReturn(
          aResponse()
            .withStatus(Status.NOT_FOUND)
        )
    )

    val connector = injector.instanceOf[PensionSchemeVarianceLockConnectorImpl]

    connector.getLockByPsa(psaId).map(schemeVariance =>
      schemeVariance shouldBe None
    )
  }

  it should "return a failed future on upstream error" in {

    server.stubFor(
      get(urlEqualTo(getLockByPsaUrl))
        .withHeader("psaId", equalTo(psaId))
        .willReturn(
          serverError
        )
    )

    val connector = injector.instanceOf[PensionSchemeVarianceLockConnectorImpl]

    recoverToExceptionIf[HttpException] {
      connector.getLockByPsa(psaId)
    } map {
      _.responseCode shouldBe Status.INTERNAL_SERVER_ERROR
    }
  }

  "getLockByScheme" should "return the valid request/response" in {

    server.stubFor(
      get(urlEqualTo(getLockBySchemeUrl))
        .withHeader("srn", equalTo(srn))
        .willReturn(
          aResponse()
            .withStatus(Status.OK)
            .withHeader("Content-Type", "application/json")
            .withBody(validSchemeVarianceLockResponse)
        )
    )

    val connector = injector.instanceOf[PensionSchemeVarianceLockConnectorImpl]

    connector.getLockByScheme(srn).map(schemeVariance =>
      schemeVariance shouldBe Some(schemeVarianceLockResponse)
    )

  }

  it should "return none if no lock found" in {

    server.stubFor(
      get(urlEqualTo(getLockBySchemeUrl))
        .withHeader("srn", equalTo(srn))
        .willReturn(
          aResponse()
            .withStatus(Status.NOT_FOUND)
        )
    )

    val connector = injector.instanceOf[PensionSchemeVarianceLockConnectorImpl]

    connector.getLockByScheme(srn).map(schemeVariance =>
      schemeVariance shouldBe None
    )
  }

  it should "return a failed future on upstream error" in {

    server.stubFor(
      get(urlEqualTo(getLockBySchemeUrl))
        .withHeader("srn", equalTo(srn))
        .willReturn(
          serverError
        )
    )

    val connector = injector.instanceOf[PensionSchemeVarianceLockConnectorImpl]

    recoverToExceptionIf[HttpException] {
      connector.getLockByScheme(srn)
    } map {
      _.responseCode shouldBe Status.INTERNAL_SERVER_ERROR
    }
  }

  "releaseLock" should "return the Lock for a valid request/response" in {

    server.stubFor(
      delete(urlEqualTo(releaseLockUrl))
        .withHeader("psaId", equalTo(psaId))
        .withHeader("srn", equalTo(srn))
        .willReturn(
          aResponse()
            .withStatus(Status.OK)
        )
    )

    val connector = injector.instanceOf[PensionSchemeVarianceLockConnectorImpl]

    connector.releaseLock(psaId, srn).map(schemeVariance =>
      schemeVariance shouldBe {}
    )

  }

  it should "return a failed future on upstream error" in {

    server.stubFor(
      delete(urlEqualTo(releaseLockUrl))
        .withHeader("psaId", equalTo(psaId))
        .withHeader("srn", equalTo(srn))
        .willReturn(
          aResponse()
            .withStatus(Status.INTERNAL_SERVER_ERROR)
        )
    )

    val connector = injector.instanceOf[PensionSchemeVarianceLockConnectorImpl]

    recoverToExceptionIf[HttpException] {
      connector.releaseLock(psaId, srn)
    } map {
      _.responseCode shouldBe Status.INTERNAL_SERVER_ERROR
    }
  }

}

object PensionSchemeVarianceLockConnectorSpec {

  private val psaId = "A2100005"
  private val srn = "00000000AA"

  private val lockUrl = s"/pensions-scheme/update-scheme/lock"
  private val getLockUrl = s"/pensions-scheme/update-scheme/get-lock"
  private val getLockByPsaUrl = s"/pensions-scheme/update-scheme/get-lock-by-psa"
  private val getLockBySchemeUrl = s"/pensions-scheme/update-scheme/get-lock-by-scheme"
  private val releaseLockUrl = s"/pensions-scheme/update-scheme/release-lock"
  private val isLockByPsaOrSchemeUrl = s"/pensions-scheme/update-scheme/isLockByPsaOrScheme"

  private val schemeVarianceLockResponse = SchemeVariance("A2100005", "00000000AA")

  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  private val validSchemeVarianceLockResponse = Json.toJson(schemeVarianceLockResponse).toString()

}
