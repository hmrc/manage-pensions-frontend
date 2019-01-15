/*
 * Copyright 2019 HM Revenue & Customs
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

import com.google.inject.{ImplementedBy, Inject}
import config.FrontendAppConfig
import models.PsaToBeRemovedFromScheme
import play.api.Logger
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import utils.HttpResponseHelper
import play.api.http.Status._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure

@ImplementedBy(classOf[PsaDeregistrationConnectorImpl])
trait PsaDeregistrationConnector {
  def deregister(psaId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext) : Future[Unit]
}

class PsaDeregistrationConnectorImpl @Inject()(http: HttpClient, config: FrontendAppConfig) extends PsaDeregistrationConnector with HttpResponseHelper {
  override def deregister(psaId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {
    val deregisterUrl = config.deregisterPsaUrl.format(psaId)
    http.DELETE(deregisterUrl) map {
      response => response.status match {
        case NO_CONTENT => ()
        case _ => handleErrorResponse("DELETE", deregisterUrl)(response)
      }
    } andThen {
      case Failure(t: Throwable) => Logger.warn("Unable to deregister PSA", t)
    }
  }
}