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

package connectors.admin

import com.google.inject.ImplementedBy
import com.google.inject.Inject
import config.FrontendAppConfig
import models.psa.remove.PsaToBeRemovedFromScheme
import play.api.Logger
import play.api.http.Status.NO_CONTENT
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.http.client.HttpClientV2
import utils.HttpResponseHelper

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Failure

@ImplementedBy(classOf[PsaRemovalConnectorImpl])
trait PsaRemovalConnector {
  def remove(psaToBeRemoved: PsaToBeRemovedFromScheme
            )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit]
}

class PsaRemovalConnectorImpl @Inject()(httpClientV2: HttpClientV2, config: FrontendAppConfig)
  extends PsaRemovalConnector
    with HttpResponseHelper {

  private val logger = Logger(classOf[PsaRemovalConnectorImpl])

  override def remove(psaToBeRemoved: PsaToBeRemovedFromScheme
                     )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {
    val removePsaUrl = url"${config.removePsaUrl}"
    httpClientV2.post(removePsaUrl)(hc)
      .withBody(Json.toJson(psaToBeRemoved))
      .execute[HttpResponse].map { response =>
        response.status match {
          case NO_CONTENT => ()
          case _ => handleErrorResponse("POST", removePsaUrl.toString)(response)
        }
      } andThen {
        case Failure(t: Throwable) => logger.warn("Unable to remove PSA from Scheme", t)
      }
  }
}
