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

import com.google.inject.Inject
import config.FrontendAppConfig
import models.invitations.Invitation
import play.api.http.Status._
import play.api.libs.json._
import play.api.libs.ws.{WSClient, WSRequest}
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.http.{HeaderCarrier, HttpException}

import scala.concurrent.{ExecutionContext, Future}

trait InvitationsCacheConnector {
  def add(invitation: Invitation)
         (implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Unit]

  def remove(pstr: String, inviteePsaId: PsaId)
            (implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Unit]

  def get(pstr: String, inviteePsaId: PsaId)
         (implicit ec: ExecutionContext, hc: HeaderCarrier): Future[List[Invitation]]

  def getForScheme(pstr: String)
                  (implicit ec: ExecutionContext, hc: HeaderCarrier): Future[List[Invitation]]

  def getForInvitee(inviteePsaId: PsaId)
                   (implicit ec: ExecutionContext, hc: HeaderCarrier): Future[List[Invitation]]
}

class InvitationsCacheConnectorImpl @Inject()(
                                               config: FrontendAppConfig,
                                               http: WSClient
                                             ) extends InvitationsCacheConnector {

  protected val addUrl = s"${config.pensionAdminUrl}/pension-administrator/invitation/add"

  protected val getUrl = s"${config.pensionAdminUrl}/pension-administrator/invitation/get"

  protected val getForSchemeUrl = s"${config.pensionAdminUrl}/pension-administrator/invitation/get-for-scheme"

  protected val getForInviteeUrl = s"${config.pensionAdminUrl}/pension-administrator/invitation/get-for-invitee"

  protected val removeUrl = s"${config.pensionAdminUrl}/pension-administrator/invitation"

  def add(invitation: Invitation)
         (implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Unit] = {
    http
      .url(addUrl)
      .withHttpHeaders(CacheConnector.headers(hc): _*)
      .post(Json.toJson(invitation))
      .flatMap {
        response =>
          response.status match {
            case OK =>
              Future.successful(())
            case _ =>
              Future.failed(new HttpException(response.body, response.status))
          }
      }
  }

  def remove(pstr: String, inviteePsaId: PsaId)
            (implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Unit] = {
    http
      .url(removeUrl)
      .withHttpHeaders(CacheConnector.headers(hc.withExtraHeaders(
        "pstr" -> pstr,
        "inviteePsaId" -> inviteePsaId.id
      )): _*)
      .delete()
      .flatMap {
        response =>
          response.status match {
            case OK =>
              Future.successful(())
            case _ =>
              Future.failed(new HttpException(response.body, response.status))
          }
      }
  }

  private def getCommon(wsRequest: WSRequest)
                       (implicit ec: ExecutionContext): Future[List[Invitation]] = {
    wsRequest
      .get()
      .flatMap {
        response =>
          response.status match {
            case NOT_FOUND =>
              Future.successful(List.empty)
            case OK =>
              Future.successful(Json.parse(response.body).as[List[Invitation]])
            case _ =>
              Future.failed(new HttpException(response.body, response.status))
          }
      }
  }

  def get(pstr: String, inviteePsaId: PsaId)
         (implicit ec: ExecutionContext, hc: HeaderCarrier): Future[List[Invitation]] =
    getCommon(
      http
        .url(getUrl)
        .withHttpHeaders(CacheConnector.headers(hc.withExtraHeaders(
          "pstr" -> pstr,
          "inviteePsaId" -> inviteePsaId.id
        )): _*)
    )

  def getForScheme(pstr: String)
                  (implicit ec: ExecutionContext, hc: HeaderCarrier): Future[List[Invitation]] =
    getCommon(
      http
        .url(getForSchemeUrl)
        .withHttpHeaders(CacheConnector.headers(hc.withExtraHeaders(
          "pstr" -> pstr
        )): _*)
    )

  def getForInvitee(inviteePsaId: PsaId)
                   (implicit ec: ExecutionContext, hc: HeaderCarrier): Future[List[Invitation]] =
    getCommon(
      http
        .url(getForInviteeUrl)
        .withHttpHeaders(CacheConnector.headers(hc.withExtraHeaders(
          "inviteePsaId" -> inviteePsaId.id
        )): _*)
    )
}
