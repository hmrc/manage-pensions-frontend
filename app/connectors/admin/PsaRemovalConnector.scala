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

package connectors.admin

import com.google.inject.ImplementedBy
import com.google.inject.Inject
import config.FrontendAppConfig
import models.PsaToBeRemovedFromScheme
import play.api.Logger
import play.api.http.Status._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import utils.HttpResponseHelper

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Failure

@ImplementedBy(classOf[PsaRemovalConnectorImpl])
trait PsaRemovalConnector {
  def remove(psaToBeRemoved: PsaToBeRemovedFromScheme)(implicit hc: HeaderCarrier, ec: ExecutionContext) : Future[Unit]
}

class PsaRemovalConnectorImpl @Inject()(http: HttpClient, config: FrontendAppConfig) extends PsaRemovalConnector with HttpResponseHelper {
  override def remove(psaToBeRemoved: PsaToBeRemovedFromScheme)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {
    http.POST[PsaToBeRemovedFromScheme, HttpResponse](config.removePsaUrl,psaToBeRemoved) map {
      response => response.status match {
        case NO_CONTENT => ()
        case _ => handleErrorResponse("POST", config.removePsaUrl)(response)
      }
    } andThen {
      case Failure(t: Throwable) => Logger.warn("Unable to remove PSA from Scheme", t)
    }
  }
}
