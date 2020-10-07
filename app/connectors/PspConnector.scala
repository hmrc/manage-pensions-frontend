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

package connectors

import com.google.inject.{ImplementedBy, Inject}
import config.FrontendAppConfig
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import utils.HttpResponseHelper

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure

@ImplementedBy(classOf[PspConnectorImpl])
trait PspConnector {

  @throws(classOf[ActiveRelationshipExistsException])
  def authorisePsp(pstr: String, pspName: String, pspId: String, clientReference: Option[String])
                  (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit]

}

class PspConnectorImpl @Inject()(http: HttpClient, config: FrontendAppConfig) extends PspConnector with HttpResponseHelper {


  override def authorisePsp(pstr: String, psaId: String, pspId: String, clientReference: Option[String])
                           (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {

    val clientRefJson = clientReference.map(cr => Json.obj("clientReference" -> cr)).getOrElse(Json.obj())
    val headerCarrier = hc.withExtraHeaders("pstr" -> pstr)
    val pspDetails = Json.obj(
      "inviteeIDType" -> "PSP",
      "inviterPSAID" -> psaId,
      "inviteeIDNumber" -> pspId) ++ clientRefJson

    val json = Json.obj(
      "pspAssociationIDsDetails" -> pspDetails,
      "pspDeclarationDetails" -> Json.obj("box1" -> true, "box2" -> true, "box3" -> true)
    )

    http.POST[JsValue, HttpResponse](config.authorisePspUrl, json)(implicitly, implicitly, headerCarrier, implicitly) map {
      response =>
        response.status match {
          case OK => ()
          case FORBIDDEN if response.body.contains("ACTIVE_RELATIONSHIP_EXISTS") => throw new ActiveRelationshipExistsException
          case _ => handleErrorResponse("POST", config.authorisePspUrl)(response)
        }
    } andThen {
      case Failure(t: Throwable) => Logger.warn("Unable to authorise psp", t)
    }
  }

}
