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

import com.google.inject.{ImplementedBy, Inject, Singleton}
import config.FrontendAppConfig
import models.ListOfSchemes
import play.api.Logger
import play.api.http.Status
import play.api.libs.json.{JsError, JsResultException, JsSuccess, Json}
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpResponse, Upstream5xxResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Try}

@ImplementedBy(classOf[ListOfSchemesConnectorImpl])
trait ListOfSchemesConnector {

  def getListOfSchemes(psaId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[ListOfSchemes]

}

@Singleton
class ListOfSchemesConnectorImpl @Inject()(http: HttpClient, config: FrontendAppConfig) extends ListOfSchemesConnector {

  def getListOfSchemes(psaId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[ListOfSchemes] = {
    val url = config.listOfSchemesUrl
    val schemeHc = hc.withExtraHeaders("psaId" -> psaId)
    http.GET[HttpResponse](url)(implicitly, schemeHc, implicitly).map { response =>
      require(response.status == Status.OK)

      val json = Json.parse(response.body)
      json.validate[ListOfSchemes] match {
        case JsSuccess(value, _) => value
        case JsError(errors) => throw new JsResultException(errors)
      }
    } andThen {
      logExceptions
    } recoverWith {
      translateExceptions()
    }
  }

  private def logExceptions: PartialFunction[Try[ListOfSchemes], Unit] = {
    case Failure(t: Throwable) => Logger.error("Unable to retrieve list of schemes", t)
  }

  private def translateExceptions(): PartialFunction[Throwable, Future[ListOfSchemes]] = {
    case ex: BadRequestException
      if ex.message.contains("INVALID_PAYLOAD")
    => Future.failed(InvalidPayloadException())
    case ex: BadRequestException
      if ex.message.contains("INVALID_CORRELATION_ID")
    => Future.failed(InvalidCorrelationIdException())
    case Upstream5xxResponse(_, Status.INTERNAL_SERVER_ERROR, _)
    => Future.failed(InternalServerErrorException())
    case Upstream5xxResponse(_, Status.SERVICE_UNAVAILABLE, _)
    => Future.failed(ServiceUnavailableException())
  }

}

sealed trait ListOfSchemesException extends Exception

case class InvalidPayloadException() extends ListOfSchemesException

case class InvalidCorrelationIdException() extends ListOfSchemesException

case class InternalServerErrorException() extends ListOfSchemesException

case class ServiceUnavailableException() extends ListOfSchemesException
