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

package connectors.scheme

import com.google.inject.{ImplementedBy, Inject, Singleton}
import config.FrontendAppConfig
import play.api.Logger
import play.api.http.Status.OK
import play.api.libs.json.{JsError, JsSuccess, Json}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import utils.{HttpResponseHelper, UserAnswers}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure

@ImplementedBy(classOf[SchemeDetailsConnectorImpl])
trait SchemeDetailsConnector {

  def getSchemeDetails(psaId: String, idNumber: String, schemeIdType: String
                      )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[UserAnswers]

  def isPsaAssociated(psaOrPspId: String, idType: String, srn: String
                     )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Boolean]]

  def getSchemeDetailsRefresh(psaId: String, idNumber: String, schemeIdType: String
                             )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit]

  def getPspSchemeDetails(pspId: String, srn: String
                         )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[UserAnswers]

}

@Singleton
class SchemeDetailsConnectorImpl @Inject()(httpClientV2: HttpClientV2, config: FrontendAppConfig)
  extends SchemeDetailsConnector
    with HttpResponseHelper {

  private val logger = Logger(classOf[SchemeDetailsConnectorImpl])

  override def getSchemeDetails(psaId: String, idNumber: String, schemeIdType: String
                               )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[UserAnswers] = {

    val schemeDetailsUrl = url"${config.schemeDetailsUrl.format(idNumber)}"
    val schemeHc = hc.withExtraHeaders("idNumber" -> idNumber, "psaId" -> psaId, "schemeIdType" -> schemeIdType)

    httpClientV2.get(schemeDetailsUrl)(schemeHc)
      .execute[HttpResponse].map { response =>
        response.status match {
          case OK =>
            val json = Json.parse(response.body)
            UserAnswers(json)
          case _ => handleErrorResponse("GET", schemeDetailsUrl.toString)(response)
        }
      } andThen {
        case Failure(t: Throwable) => logger.warn("Unable to get scheme details", t)
      }
  }

  override def isPsaAssociated(psaOrPspId: String, idType: String, srn: String
                              )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Boolean]] = {

    val isSchemeAssociatedUrl = url"${config.isSchemeAssociatedUrl}"
    val idHeader = if(idType == "psa") "psaId" -> psaOrPspId else "pspId" -> psaOrPspId
    val schemeHc = hc.withExtraHeaders(idHeader, "schemeReferenceNumber" -> srn)

    httpClientV2.get(isSchemeAssociatedUrl)(schemeHc)
      .execute[HttpResponse].map { response =>
        response.status match {
          case OK =>
            response.json.validate[Boolean] match {
              case JsSuccess(isDataChanged, _) => Some(isDataChanged)
              case JsError(errors) =>
                logger.warn(s"Unable to de-serialise the response $errors")
                None
            }
          case _ => handleErrorResponse("GET", isSchemeAssociatedUrl.toString)(response)
        }
      } andThen {
        case Failure(t: Throwable) => logger.warn("Unable to get  if this psaOrPspId is associated with the scheme details", t)
      }
  }

  override def getSchemeDetailsRefresh(psaId: String, idNumber: String, schemeIdType: String
                               )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {

    val schemeDetailsUrl = url"${config.schemeDetailsUrl.format(idNumber)}"
    val schemeHc = hc.withExtraHeaders(
        "idNumber" -> idNumber,
        "psaId" -> psaId,
        "schemeIdType" -> schemeIdType,
        "refreshData" -> "true"
      )

    httpClientV2.get(schemeDetailsUrl)(schemeHc)
      .execute[HttpResponse].map { response =>
        response.status match {
          case OK => ()
          case _ => handleErrorResponse("GET", schemeDetailsUrl.toString)(response)
        }
      } andThen {
        case Failure(t: Throwable) => logger.warn("Unable to get scheme details", t)
      }
  }

  override def getPspSchemeDetails(pspId: String, srn: String
                                  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[UserAnswers] = {

    val pspSchemeDetailsUrl = url"${config.pspSchemeDetailsUrl.format(srn)}"
    val schemeHc = hc.withExtraHeaders("srn" -> srn, "pspId" -> pspId)

    httpClientV2.get(pspSchemeDetailsUrl)(schemeHc)
      .execute[HttpResponse].map { response =>
        response.status match {
          case OK => UserAnswers(Json.parse(response.body))
          case _ => handleErrorResponse("GET", pspSchemeDetailsUrl.toString)(response)
        }
      } andThen {
        case Failure(t: Throwable) => logger.warn("Unable to psp get scheme details", t)
      }
  }

}
