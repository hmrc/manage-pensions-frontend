/*
 * Copyright 2018 HM Revenue & Customs
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

import com.google.inject.{ImplementedBy, Inject, Singleton}
import config.FrontendAppConfig
import models.PsaSchemeDetails
import play.api.Logger
import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.libs.json.{JsError, JsResultException, JsSuccess, Json}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import utils.HttpResponseHelper

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure

@ImplementedBy(classOf[SchemeDetailsConnectorImpl])
trait SchemeDetailsConnector {

  def getSchemeDetails(schemeIdType: String, idNumber: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[PsaSchemeDetails]

}

@Singleton
class SchemeDetailsConnectorImpl @Inject()(http: HttpClient, config: FrontendAppConfig) extends
  SchemeDetailsConnector with HttpResponseHelper {

  def getSchemeDetails(schemeIdType: String, idNumber: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[PsaSchemeDetails] = {

    val url = config.schemeDetailsUrl.format(schemeIdType, idNumber)

    http.GET[HttpResponse](url)(implicitly, hc, implicitly).map { response =>
      response.status match {
        case OK =>
          val json = Json.parse(response.body)
          json.validate[PsaSchemeDetails] match {
            case JsSuccess(value, _) => value
            case JsError(errors) => throw new JsResultException(errors)
          }
        case _ => handleErrorResponse("GET", url)(response)
      }
    } andThen {
      case Failure(t: Throwable) => Logger.warn("Unable to get scheme details", t)
    }
  }
}

