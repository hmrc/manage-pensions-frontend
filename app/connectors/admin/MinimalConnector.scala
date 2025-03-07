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

import com.google.inject.{ImplementedBy, Inject}
import config.FrontendAppConfig
import models.{MinimalPSAPSP, SchemeReferenceNumber}
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json.{JsError, JsResultException, JsSuccess, Json}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import utils.HttpResponseHelper

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure

@ImplementedBy(classOf[MinimalConnectorImpl])
trait MinimalConnector {

  def getMinimalPsaDetails()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[MinimalPSAPSP]

  def getEmailInvitation(id: String, idType: String, name: String, srn: SchemeReferenceNumber)
                        (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[String]]

  def getMinimalPspDetails()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[MinimalPSAPSP]

  def getPsaNameFromPsaID()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[String]]

  def getNameFromPspID()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[String]]
}

class NoMatchFoundException extends Exception

class MinimalConnectorImpl @Inject()(httpClientV2: HttpClientV2, config: FrontendAppConfig)
  extends MinimalConnector with HttpResponseHelper {

  private val logger = Logger(classOf[MinimalConnectorImpl])

  override def getMinimalPsaDetails()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[MinimalPSAPSP] =
    getMinimalDetails(hc.withExtraHeaders("loggedInAsPsa" -> "true"))

  override def getMinimalPspDetails()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[MinimalPSAPSP] =
    getMinimalDetails(hc.withExtraHeaders("loggedInAsPsa" -> "false"))

  override def getEmailInvitation(id: String,idType: String, name: String, srn: SchemeReferenceNumber)
                                 (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[String]] ={
    retrieveEmailDetails(srn, hc.withExtraHeaders("id" -> id, "idType" -> idType, "name" -> name))(ec).map {
      case None => throw new NoMatchFoundException
      case Some(m) => Some(m)
    } andThen {
      case Failure(t: Throwable) => logger.warn("Unable to get email details", t)
    }
  }

  private def getMinimalDetails(hc: HeaderCarrier)
                               (implicit ec: ExecutionContext): Future[MinimalPSAPSP] = {
    retrieveMinimalDetails(hc)(ec).map {
      case None => throw new NoMatchFoundException
      case Some(m) => m
    } andThen {
      case Failure(_: DelimitedAdminException) => ()
      case Failure(_: DelimitedPractitionerException) => ()
      case Failure(_: PspUserNameNotMatchedException) => ()
      case Failure(t: Throwable) => logger.warn("Unable to get minimal details", t)
    }
  }
  private def retrieveEmailDetails(srn: SchemeReferenceNumber, hc: HeaderCarrier)(implicit ec: ExecutionContext): Future[Option[String]] = {
    val emailDetailsUrl = url"${config.emailDetailsUrl}/${srn.id}"

    httpClientV2.get(emailDetailsUrl)(hc)
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK => Option(response.body)
          case FORBIDDEN if response.body.contains(pspUserNotMatchedErrorMsg) => throw new PspUserNameNotMatchedException
          case FORBIDDEN if response.body.contains(pspDelimitedErrorMsg) => throw new DelimitedPractitionerException
          case NOT_FOUND => None
          case _ => handleErrorResponse("GET", emailDetailsUrl.toString)(response)
        }
      }
  }

  private def retrieveMinimalDetails(hc: HeaderCarrier)(implicit ec: ExecutionContext): Future[Option[MinimalPSAPSP]] = {
    val minimalPsaDetailsUrl = url"${config.minimalPsaDetailsUrl}"

    httpClientV2.get(minimalPsaDetailsUrl)(hc)
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK =>
            Json.parse(response.body).validate[MinimalPSAPSP] match {
              case JsSuccess(value, _) => Some(value)
              case JsError(errors) => throw JsResultException(errors)
            }
          case NOT_FOUND => None
          case FORBIDDEN if response.body.contains(pspDelimitedErrorMsg) => throw new DelimitedPractitionerException
          case FORBIDDEN if response.body.contains(delimitedErrorMsg) => throw new DelimitedAdminException
          case _ => handleErrorResponse("GET", minimalPsaDetailsUrl.toString)(response)
        }
      }
  }

  private val delimitedErrorMsg: String = "DELIMITED_PSAID"
  private val pspDelimitedErrorMsg: String = "DELIMITED_PSPID"
  private val pspUserNotMatchedErrorMsg: String = "Provided user's name doesn't match with stored user's name"
  override def getPsaNameFromPsaID()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[String]] =
    getMinimalPsaDetails().map(MinimalPSAPSP.getNameFromId)

  override def getNameFromPspID()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[String]] = {
    retrieveMinimalDetails(hc.withExtraHeaders("loggedInAsPsa" -> "false"))
      .map(_.flatMap( MinimalPSAPSP.getNameFromId)) andThen {
      case Failure(_: DelimitedPractitionerException) => ()
      case Failure(t: Throwable) => logger.warn("Unable to get minimal details", t)
    }
  }
}

class DelimitedPractitionerException
  extends Exception("The practitioner has already de-registered. The minimal details API has returned a DELIMITED PSP response")
class DelimitedAdminException extends
  Exception("The administrator has already de-registered. The minimal details API has returned a DELIMITED PSA response")
class PspUserNameNotMatchedException  extends
  Exception("Provided user's name doesn't match with stored user's name")
