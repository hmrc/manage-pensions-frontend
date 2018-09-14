/*
 * Copyright 2018 HM Revenue & Customs
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
import models.{AcceptedInvitation, Invitation}
import play.api.Logger
import play.api.http.Status._
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import utils.HttpResponseHelper
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure

@ImplementedBy(classOf[InvitationConnectorImpl])
trait InvitationConnector {

  def invite(invitation: Invitation)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit.type]

  def acceptInvite(acceptedInvitation: AcceptedInvitation)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit]

}

abstract class InvitationException extends Exception
class PsaIdInvalidException extends InvitationException
class PsaIdNotFoundException extends InvitationException

abstract class AcceptInvitationException extends Exception
class PstrInvalidException extends AcceptInvitationException
class InvalidInvitationPayloadException extends AcceptInvitationException
class InviteePsaIdInvalidException extends AcceptInvitationException
class InviterPsaIdInvalidException extends AcceptInvitationException
class ActiveRelationshipExistsException extends AcceptInvitationException

class InvitationConnectorImpl @Inject()(http: HttpClient, config: FrontendAppConfig) extends InvitationConnector with HttpResponseHelper {

  override def invite(invitation: Invitation)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit.type] = {

    http.POST[Invitation, HttpResponse](config.inviteUrl, invitation) map {
      response =>
        response.status match {
          case CREATED => Unit
          case BAD_REQUEST if response.body.contains("INVALID_PSAID") => throw new PsaIdInvalidException()
          case NOT_FOUND => throw new PsaIdNotFoundException()
          case _ => handleErrorResponse("POST", config.inviteUrl)(response)
        }
    } andThen {
      case Failure(t: Throwable) => Logger.warn("Unable to invite PSA to administer scheme", t)
    }
  }

  def handleBadResponse(response: String): Unit = {
    val InvalidPstrPattern = "(.*INVALID_PSTR.*)".r
    val InvalidPayloadPattern = "(.*INVALID_PAYLOAD.*)".r
    val InvalidInviteePattern = "(.*INVALID_INVITEE_PSAID.*)".r
    val InvalidInviterPattern = "(.*INVALID_INVITER_PSAID.*)".r

    response match {
      case InvalidPstrPattern(_) => throw new PstrInvalidException
      case InvalidPayloadPattern(_) => throw new InvalidInvitationPayloadException
      case InvalidInviteePattern(_) => throw new InviteePsaIdInvalidException
      case InvalidInviterPattern(_) => throw new InviterPsaIdInvalidException
    }
  }

  override def acceptInvite(acceptedInvitation: AcceptedInvitation)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {

    http.POST[AcceptedInvitation, HttpResponse](config.acceptInvitationUrl, acceptedInvitation) map {
      response =>
        response.status match {
          case OK => ()
          case BAD_REQUEST => handleBadResponse(response.body)
          case CONFLICT if response.body.contains("ACTIVE_RELATIONSHIP_EXISTS") => throw new ActiveRelationshipExistsException
          case _ => handleErrorResponse("POST", config.acceptInvitationUrl)(response)
        }
    } andThen {
      case Failure(t: Throwable) => Logger.warn("Unable to accept invitation to administer a scheme", t)
    }
  }

}
