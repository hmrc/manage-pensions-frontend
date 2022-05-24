/*
 * Copyright 2022 HM Revenue & Customs
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
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HttpClient, _}
import utils.{HttpResponseHelper, UserAnswers}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure

@ImplementedBy(classOf[SchemeDetailsConnectorImpl])
trait SchemeDetailsConnector {

  def getSchemeDetails(psaId: String, idNumber: String, schemeIdType: String)
                      (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[UserAnswers]

  def getSchemeDetailsRefresh(psaId: String, idNumber: String, schemeIdType: String)
                      (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit]

  def getPspSchemeDetails(pspId: String, srn: String)
                         (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[UserAnswers]

}

@Singleton
class SchemeDetailsConnectorImpl @Inject()(http: HttpClient, config: FrontendAppConfig)
  extends SchemeDetailsConnector
    with HttpResponseHelper {

  private val logger = Logger(classOf[SchemeDetailsConnectorImpl])

  override def getSchemeDetails(
                                 psaId: String,
                                 idNumber: String,
                                 schemeIdType: String
                               )(
                                 implicit hc: HeaderCarrier,
                                 ec: ExecutionContext
                               ): Future[UserAnswers] = {

    val url = config.schemeDetailsUrl
    val schemeHc =
      hc.withExtraHeaders(
        "idNumber" -> idNumber,
        "psaId" -> psaId,
        "schemeIdType" -> schemeIdType
      )
    http.GET[HttpResponse](url)(implicitly, schemeHc, implicitly).map { response =>
      response.status match {
        case OK =>
          val json = Json.parse(response.body)
          UserAnswers(json)
        case _ => handleErrorResponse("GET", url)(response)
      }
    } andThen {
      case Failure(t: Throwable) => logger.warn("Unable to get scheme details", t)
    }
  }

  override def getSchemeDetailsRefresh(
                                 psaId: String,
                                 idNumber: String,
                                 schemeIdType: String
                               )(
                                 implicit hc: HeaderCarrier,
                                 ec: ExecutionContext
                               ): Future[Unit] = {

    val url = config.schemeDetailsUrl
    val schemeHc =
      hc.withExtraHeaders(
        "idNumber" -> idNumber,
        "psaId" -> psaId,
        "schemeIdType" -> schemeIdType,
        "refreshData" -> "true"
      )
    http.GET[HttpResponse](url)(implicitly, schemeHc, implicitly).map { response =>
      response.status match {
        case OK => ()
        case _ => handleErrorResponse("GET", url)(response)
      }
    } andThen {
      case Failure(t: Throwable) => logger.warn("Unable to get scheme details", t)
    }
  }

  override def getPspSchemeDetails(pspId: String, srn: String)
                                  (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[UserAnswers] = {

    val url = config.pspSchemeDetailsUrl
    val schemeHc = hc.withExtraHeaders("srn" -> srn, "pspId" -> pspId)

    http.GET[HttpResponse](url)(implicitly, schemeHc, implicitly).map { response =>
      response.status match {
        case OK => UserAnswers(Json.parse(response.body))
        case _ => handleErrorResponse("GET", url)(response)
      }
    } andThen {
      case Failure(t: Throwable) => logger.warn("Unable to psp get scheme details", t)
    }
  }
}

