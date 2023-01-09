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

package connectors.admin

import com.google.inject.{ImplementedBy, Inject}
import config.FrontendAppConfig
import models.psa.SubscriptionDetails
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import utils.HttpResponseHelper

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure

abstract class SubscriptionException extends Exception

class PsaIdInvalidSubscriptionException extends SubscriptionException

class CorrelationIdInvalidSubscriptionException extends SubscriptionException

class PsaIdNotFoundSubscriptionException extends SubscriptionException

@ImplementedBy(classOf[SubscriptionConnectorImpl])
trait SubscriptionConnector {

  def getSubscriptionDetails(psaId: String)
                            (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[SubscriptionDetails]
}

class SubscriptionConnectorImpl @Inject()(http: HttpClient, config: FrontendAppConfig)
  extends SubscriptionConnector
    with HttpResponseHelper {

  private val logger = Logger(classOf[SubscriptionConnectorImpl])

  override def getSubscriptionDetails(psaId: String)
                                     (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[SubscriptionDetails] = {

    val psaIdHC = hc.withExtraHeaders("psaId" -> psaId)

    val url = config.subscriptionDetailsUrl

    http.GET[HttpResponse](url)(implicitly, psaIdHC, implicitly) map { response =>

      response.status match {
        case OK => validateJson(response.json)
        case BAD_REQUEST if response.body.contains("INVALID_PSAID") => throw new PsaIdInvalidSubscriptionException
        case BAD_REQUEST if response.body.contains("INVALID_CORRELATIONID") => throw new CorrelationIdInvalidSubscriptionException
        case NOT_FOUND => throw new PsaIdNotFoundSubscriptionException
        case _ => handleErrorResponse("GET", config.subscriptionDetailsUrl)(response)
      }
    } andThen {
      case Failure(t: Throwable) => logger.warn("Unable to get PSA subscription details", t)
    }

  }

  private def validateJson(json: JsValue): SubscriptionDetails = {
    json.validate[SubscriptionDetails] match {
      case JsSuccess(value, _) => value
      case JsError(errors) => throw new JsResultException(errors)
    }
  }
}
