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

import com.google.inject.Inject
import config.FrontendAppConfig
import models.invitations.Invitation
import play.api.http.Status._
import play.api.libs.json._
import play.api.libs.ws.writeableOf_JsValue
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpException, HttpResponse, StringContextOps}

import scala.concurrent.{ExecutionContext, Future}

trait InvitationsCacheConnector {
  def add(invitation: Invitation)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Unit]

  def remove(pstr: String, inviteePsaId: PsaId)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Unit]

  def get(pstr: String, inviteePsaId: PsaId)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[List[Invitation]]

  def getForScheme(pstr: String)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[List[Invitation]]

  def getForInvitee(inviteePsaId: PsaId)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[List[Invitation]]
}

class InvitationsCacheConnectorImpl @Inject()(config: FrontendAppConfig, httpClientV2: HttpClientV2) extends InvitationsCacheConnector {

  protected val addUrl = url"${config.pensionAdminUrl}/pension-administrator/invitation/add"

  protected val getUrl = url"${config.pensionAdminUrl}/pension-administrator/invitation/get"

  protected val getForSchemeUrl = url"${config.pensionAdminUrl}/pension-administrator/invitation/get-for-scheme"

  protected val getForInviteeUrl = url"${config.pensionAdminUrl}/pension-administrator/invitation/get-for-invitee"

  protected val removeUrl = url"${config.pensionAdminUrl}/pension-administrator/invitation"

  def add(invitation: Invitation
         )(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Unit] = {
    httpClientV2.post(addUrl)
      .setHeader(CacheConnector.headers(hc) *)
      .withBody(Json.toJson(invitation))
      .execute[HttpResponse].flatMap {
        response =>
          response.status match {
            case OK =>
              Future.successful(())
            case _ =>
              Future.failed(new HttpException(response.body, response.status))
          }
      }
  }

  def remove(pstr: String, inviteePsaId: PsaId
            )(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Unit] = {
    val hcExtraHeaders = hc.withExtraHeaders("pstr" -> pstr, "inviteePsaId" -> inviteePsaId.id)

    httpClientV2.delete(removeUrl)
      .setHeader(CacheConnector.headers(hcExtraHeaders) *)
      .execute[HttpResponse].flatMap {
        response =>
          response.status match {
            case OK =>
              Future.successful(())
            case _ =>
              Future.failed(new HttpException(response.body, response.status))
          }
      }
  }

  private def getCommon(response: HttpResponse): Future[List[Invitation]] =
    response.status match {
      case NOT_FOUND =>
        Future.successful(List.empty)
      case OK =>
        Future.successful(Json.parse(response.body).as[List[Invitation]])
      case _ =>
        Future.failed(new HttpException(response.body, response.status))
    }

  def get(pstr: String, inviteePsaId: PsaId)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[List[Invitation]] = {
    val hcExtraHeaders = hc.withExtraHeaders("pstr" -> pstr, "inviteePsaId" -> inviteePsaId.id)

    httpClientV2.get(getUrl)
      .setHeader(CacheConnector.headers(hcExtraHeaders) *)
      .execute[HttpResponse]
      .flatMap(getCommon)
  }

  def getForScheme(pstr: String)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[List[Invitation]] = {
    val hcExtraHeaders = hc.withExtraHeaders("pstr" -> pstr)

    httpClientV2.get(getForSchemeUrl)
      .setHeader(CacheConnector.headers(hcExtraHeaders) *)
      .execute[HttpResponse]
      .flatMap(getCommon)
  }

  def getForInvitee(inviteePsaId: PsaId)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[List[Invitation]] = {
    val hcExtraHeaders = hc.withExtraHeaders("inviteePsaId" -> inviteePsaId.id)

    httpClientV2.get(getForInviteeUrl)
      .setHeader(CacheConnector.headers(hcExtraHeaders) *)
      .execute[HttpResponse]
      .flatMap(getCommon)
  }

}
