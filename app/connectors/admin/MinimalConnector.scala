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
import models.MinimalPSAPSP
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json.{JsError, JsResultException, JsSuccess, Json}
import uk.gov.hmrc.http.{HttpClient, _}
import utils.HttpResponseHelper

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure

@ImplementedBy(classOf[MinimalConnectorImpl])
trait MinimalConnector {

  def getMinimalPsaDetails(psaId: String)
                          (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[MinimalPSAPSP]

  def getMinimalPspDetails(pspId: String)
                          (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[MinimalPSAPSP]

  def getPsaNameFromPsaID(psaId: String)
                         (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[String]]

  def getNameFromPspID(pspId: String)
                      (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[String]]
}

class NoMatchFoundException extends Exception

class MinimalConnectorImpl @Inject()(http: HttpClient, config: FrontendAppConfig)
  extends MinimalConnector
    with HttpResponseHelper {

  private val logger = Logger(classOf[MinimalConnectorImpl])

  override def getMinimalPsaDetails(psaId: String)
                                   (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[MinimalPSAPSP] =
    getMinimalDetails(hc.withExtraHeaders("psaId" -> psaId))

  override def getMinimalPspDetails(pspId: String)
                                   (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[MinimalPSAPSP] =
    getMinimalDetails(hc.withExtraHeaders("pspId" -> pspId))

  private def getMinimalDetails(hc: HeaderCarrier)
                               (implicit ec: ExecutionContext): Future[MinimalPSAPSP] = {
    retrieveMinimalDetails(hc)(ec).map {
      case None => throw new NoMatchFoundException
      case Some(m) => m
    } andThen {
       case Failure(t: Throwable) => logger.warn("Unable to get minimal details", t)
    }
  }

  private def retrieveMinimalDetails(hc: HeaderCarrier)
    (implicit ec: ExecutionContext): Future[Option[MinimalPSAPSP]] =
    http.GET[HttpResponse](config.minimalPsaDetailsUrl)(implicitly, hc, implicitly) map { response =>
      response.status match {
        case OK =>
          Json.parse(response.body).validate[MinimalPSAPSP] match {
            case JsSuccess(value, _) => Some(value)
            case JsError(errors) => throw JsResultException(errors)
          }
        case NOT_FOUND => None
        case FORBIDDEN if response.body.contains(delimitedErrorMsg) => throw new DelimitedAdminException
        case _ => handleErrorResponse("GET", config.minimalPsaDetailsUrl)(response)
      }
    }

  val delimitedErrorMsg: String = "DELIMITED_PSAID"

  override def getPsaNameFromPsaID(psaId: String)
                                  (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[String]] =
    getMinimalPsaDetails(psaId).map(MinimalPSAPSP.getNameFromId)

  override def getNameFromPspID(pspId: String)
                               (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[String]] = {
    retrieveMinimalDetails(hc.withExtraHeaders("pspId" -> pspId))
      .map(_.flatMap( MinimalPSAPSP.getNameFromId)) andThen {
      case Failure(t: Throwable) => logger.warn("Unable to get minimal details", t)
    }
  }
}

class DelimitedAdminException extends
  Exception("The administrator has already de-registered. The minimal details API has returned a DELIMITED PSA response")
