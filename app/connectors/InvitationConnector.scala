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
import models.Invitation
import play.api.Logger
import play.api.http.Status._
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import utils.HttpResponseHelper

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure

@ImplementedBy(classOf[InvitationConnectorImpl])
trait InvitationConnector {

  def invite(invitation: Invitation)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit.type ]

}

abstract class InvitationException extends Exception
class PsaIdInvalidException extends InvitationException
class CorrelationIdInvalidException extends InvitationException
class PsaIdNotFoundException extends InvitationException

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

}
