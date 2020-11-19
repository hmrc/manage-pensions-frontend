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

import com.google.inject.ImplementedBy
import com.google.inject.Inject
import com.google.inject.Singleton
import config.FrontendAppConfig
import play.api.Logger
import play.api.http.Status.OK
import play.api.libs.json.Json
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import utils.HttpResponseHelper
import utils.UserAnswers

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Failure

@ImplementedBy(classOf[SchemeDetailsConnectorImpl])
trait SchemeDetailsConnector {

  def getSchemeDetails(psaId: String, schemeIdType: String, idNumber: String)
                      (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[UserAnswers]

}

@Singleton
class SchemeDetailsConnectorImpl @Inject()(http: HttpClient, config: FrontendAppConfig)
  extends SchemeDetailsConnector
    with HttpResponseHelper {

  def getSchemeDetails(psaId: String, schemeIdType: String, idNumber: String)
                      (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[UserAnswers] = {

    val url = config.schemeDetailsUrl
    val schemeHc =
      hc.withExtraHeaders(
        "schemeIdType" -> schemeIdType,
        "idNumber" -> idNumber,
        "PSAId" -> psaId
      )
    http.GET[HttpResponse](url)(implicitly, schemeHc, implicitly).map { response =>
      response.status match {
        case OK =>
          val json = Json.parse(response.body)
          UserAnswers(json)
        case _ => handleErrorResponse("GET", url)(response)
      }
    } andThen {
      case Failure(t: Throwable) => Logger.warn("Unable to get scheme details", t)
    }
  }
}

