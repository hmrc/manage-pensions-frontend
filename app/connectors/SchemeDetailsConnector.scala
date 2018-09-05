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
import play.api.http.Status
import play.api.libs.json.{JsError, JsResultException, JsSuccess, Json}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import utils.HttpResponseHelper

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Try}

@ImplementedBy(classOf[SchemeDetailsConnectorImpl])
trait SchemeDetailsConnector {

  def getSchemeDetails(schemeIdType: String, idNumber: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[PsaSchemeDetails]

}

@Singleton
class SchemeDetailsConnectorImpl @Inject()(http: HttpClient, config: FrontendAppConfig) extends SchemeDetailsConnector {

  def getSchemeDetails(schemeIdType: String, idNumber: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[PsaSchemeDetails] = {

    val url = config.schemeDetailsUrl.format(schemeIdType, idNumber)

    http.GET[HttpResponse](url)(implicitly, hc, implicitly).map { response =>
        require(response.status == Status.OK)

        val json = Json.parse(response.body)
        json.validate[PsaSchemeDetails] match {
          case JsSuccess(value, _) => value
          case JsError(errors) => throw new JsResultException(errors)
        }
    } andThen {
      logExceptions
    } recoverWith {
      translateExceptions()
    }
  }


  private def translateExceptions(): PartialFunction[Throwable, Future[PsaSchemeDetails]] = {
    case ex: BadRequestException
      if ex.message.contains("INVALID_IDTYPE")
    => Future.failed(InvalidSchemeIdTypeException())
    case ex: BadRequestException
      if ex.message.contains("INVALID_SRN")
    => Future.failed(InvalidIdException())
    case ex: BadRequestException
    if ex.message.contains("INVALID_PSTR")
    => Future.failed(InvalidIdException())
    case ex: BadRequestException
      if ex.message.contains("INVALID_CORRELATION_ID")
    => Future.failed(InvalidCorrelationException())
    case Upstream5xxResponse(_, Status.INTERNAL_SERVER_ERROR, _)
    => Future.failed(InternalServerErrorException())
    case Upstream5xxResponse(_, Status.SERVICE_UNAVAILABLE, _)
    => Future.failed(ServiceUnavailableException())
//    case _
//    => handleErrorResponse("GET", config.schemeDetailsUrl)(_)
  }

    private def logExceptions: PartialFunction[Try[PsaSchemeDetails], Unit] = {
      case Failure(t: Throwable) => Logger.warn("Unable to get scheme details", t)
    }

}

sealed trait SchemeDetailsException extends Exception
case class InvalidSchemeIdTypeException() extends SchemeDetailsException
case class InvalidIdException() extends SchemeDetailsException
case class InvalidCorrelationException() extends SchemeDetailsException
