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

package connectors

import com.google.inject.{ImplementedBy, Inject}
import config.FrontendAppConfig
import models.SchemeReferenceNumber
import models.psp.UpdateClientReferenceRequest
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json.{JsError, JsResultException, JsSuccess, Json}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import utils.HttpResponseHelper

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure

@ImplementedBy(classOf[UpdateClientReferenceConnectorImpl])
trait UpdateClientReferenceConnector {

  def updateClientReference(updateClientReferenceRequest: UpdateClientReferenceRequest,userAction: String
      ,srn: SchemeReferenceNumber)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[String]
}

class UpdateClientReferenceConnectorImpl @Inject()(httpClientV2: HttpClientV2, config: FrontendAppConfig)
  extends UpdateClientReferenceConnector
    with HttpResponseHelper {

  private val logger = Logger(classOf[UpdateClientReferenceConnectorImpl])

  override def updateClientReference(updateClientReferenceRequest: UpdateClientReferenceRequest,userAction: String,
    srn: SchemeReferenceNumber)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[String] = {

    val updateClientReferenceUrl = url"${config.updateClientReferenceUrl}/${srn.id}"
    val headers = Seq("pspId" -> updateClientReferenceRequest.pspId,
      "pstr" -> updateClientReferenceRequest.pstr,
      "clientReference" -> updateClientReferenceRequest.clientReference.getOrElse(""),
      "userAction"-> userAction)

    println("============================++++++++++++++++++++>>>>>>>>>>>>>>")

    httpClientV2.post(updateClientReferenceUrl)
      . setHeader(headers:_*)
      .execute[HttpResponse] map {
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
