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

package connectors

import com.google.inject.{ImplementedBy, Inject}
import config.FrontendAppConfig
import models.psp.UpdateClientReferenceRequest
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json.{JsError, JsResultException, JsSuccess, Json}
import uk.gov.hmrc.http.{HttpClient, _}
import utils.HttpResponseHelper

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure

@ImplementedBy(classOf[UpdateClientReferenceConnectorImpl])
trait UpdateClientReferenceConnector {

  def updateClientReference(updateClientReferenceRequest: UpdateClientReferenceRequest,userAction: String)
                           (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[String]
}


class UpdateClientReferenceConnectorImpl @Inject()(http: HttpClient, config: FrontendAppConfig)
  extends UpdateClientReferenceConnector
    with HttpResponseHelper {

  private val logger = Logger(classOf[UpdateClientReferenceConnectorImpl])

  override def updateClientReference(updateClientReferenceRequest: UpdateClientReferenceRequest,userAction: String)
                                    (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[String] = {

    http.POST[UpdateClientReferenceRequest, HttpResponse](config.updateClientReferenceUrl, updateClientReferenceRequest,Seq("userAction"-> userAction)) map {
      response =>
        response.status match {
          case OK => val json = Json.parse(response.body)
            (json \ "status").validate[String] match {
              case JsSuccess(value, _) => value
              case JsError(errors) => throw JsResultException(errors)
            }
          case _ => handleErrorResponse("POST", config.updateClientReferenceUrl)(response)
        }
    } andThen {
      case Failure(t: Throwable) => logger.warn("Unable to update Client Reference", t)
    }
  }


}
