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
import models.DeAuthorise
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2
import utils.HttpResponseHelper

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure

@ImplementedBy(classOf[PspConnectorImpl])
trait PspConnector {

  @throws(classOf[ActiveRelationshipExistsException])
  def authorisePsp(pstr: String, psaId: String, pspId: String, clientReference: Option[String]
                  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit]

  def deAuthorise(pstr: String, deAuthorise: DeAuthorise
                 )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse]

}

abstract class DeAuthorisationException extends Exception

class DuplicateSubmissionException extends DeAuthorisationException

class PspConnectorImpl @Inject()(httpClientV2: HttpClientV2, config: FrontendAppConfig)
  extends PspConnector
    with HttpResponseHelper {

  private val logger = Logger(classOf[PspConnectorImpl])

  override def authorisePsp(pstr: String, psaId: String, pspId: String, clientReference: Option[String]
                           )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {

    val authorisePspUrl = url"${config.authorisePspUrl}"
    val clientRefJson = clientReference.map(cr => Json.obj("clientReference" -> cr)).getOrElse(Json.obj())
    val headerCarrier = hc.withExtraHeaders("pstr" -> pstr)
    val pspDetails = Json.obj(
      "inviteeIDType" -> "PSPID",
      "inviterPSAID" -> psaId,
      "inviteeIDNumber" -> pspId) ++ clientRefJson

    val json = Json.obj(
      "pspAssociationIDsDetails" -> pspDetails,
      "pspDeclarationDetails" -> Json.obj("box1" -> true, "box2" -> true, "box3" -> true)
    )

    httpClientV2.post(authorisePspUrl)(headerCarrier)
      .withBody(json)
      .execute[HttpResponse].map {
          response =>
            response.status match {
              case OK => ()
              case FORBIDDEN if response.body.contains("ACTIVE_RELATIONSHIP_EXISTS") => throw new ActiveRelationshipExistsException
              case _ => handleErrorResponse("POST", config.authorisePspUrl)(response)
            }
        } andThen {
        case Failure(_: ActiveRelationshipExistsException) =>
        case Failure(t: Throwable) => logger.warn("Unable to authorise psp", t)
      }
  }

  override def deAuthorise(pstr: String, deAuthorise: DeAuthorise
                          )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {

    val headerCarrier = hc.withExtraHeaders("pstr" -> pstr)
    val deAuthorisePspUrl = url"${config.deAuthorisePspUrl}"

    httpClientV2.post(deAuthorisePspUrl)(headerCarrier)
      .withBody(Json.toJson(deAuthorise))
      .execute[HttpResponse] map {
        response =>
          response.status match {
            case OK => response
            case CONFLICT if response.body.contains("DUPLICATE_SUBMISSION") => throw new DuplicateSubmissionException
            case _ => handleErrorResponse("POST", deAuthorisePspUrl.toString)(response)
          }
      } andThen {
        case Failure(t: Throwable) => logger.warn("Unable to de-authorise psp", t)
      }
  }
}
