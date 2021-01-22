/*
 * Copyright 2021 HM Revenue & Customs
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
import models.MinimalPSAPSP
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json.JsError
import play.api.libs.json.JsResultException
import play.api.libs.json.JsSuccess
import play.api.libs.json.Json
import uk.gov.hmrc.http.NotFoundException
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.HttpClient
import utils.HttpResponseHelper

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
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
                               (implicit ec: ExecutionContext): Future[MinimalPSAPSP] =
    http.GET[HttpResponse](config.minimalPsaDetailsUrl)(implicitly, hc, implicitly) map { response =>

      response.status match {
        case OK =>
          Json.parse(response.body).validate[MinimalPSAPSP] match {
            case JsSuccess(value, _) => value
            case JsError(errors) => throw JsResultException(errors)
          }
        case NOT_FOUND if response.body.contains("no match found") => throw new NoMatchFoundException
        case _ => handleErrorResponse("GET", config.minimalPsaDetailsUrl)(response)
      }
    } andThen {
      case Failure(t: Throwable) => logger.warn("Unable to get minimal details", t)
    }

  override def getPsaNameFromPsaID(psaId: String)
                                  (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[String]] =
    getMinimalPsaDetails(psaId).map(getNameFromId)

  override def getNameFromPspID(pspId: String)
                               (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[String]] =
    getMinimalPspDetails(pspId).map(getNameFromId) recoverWith {
      case _: NotFoundException => Future.successful(None)
    }

  private def getNameFromId(minDetails: MinimalPSAPSP): Option[String] =
    (minDetails.individualDetails, minDetails.organisationName) match {
      case (Some(individual), None) => Some(individual.fullName)
      case (None, Some(org)) => Some(s"$org")
      case _ => None
    }

}
