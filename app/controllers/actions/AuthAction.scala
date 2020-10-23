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

package controllers.actions

import com.google.inject.ImplementedBy
import com.google.inject.Inject
import config.FrontendAppConfig
import controllers.routes
import models.AuthEntity
import models.AuthEntity.PSA
import models.requests.AuthenticatedRequest
import models.OtherUser
import models.UserType
import play.api.mvc.Results._
import play.api.mvc._
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.domain.PspId
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class AuthImpl(override val authConnector: AuthConnector,
  config: FrontendAppConfig,
  val parser: BodyParsers.Default,
  authEntity: AuthEntity)
  (implicit val executionContext: ExecutionContext) extends Auth with
  AuthorisedFunctions {

  override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

    authorised().retrieve(Retrievals.externalId and
      Retrievals.allEnrolments and Retrievals.affinityGroup) {
      case Some(id) ~ enrolments ~ Some(affinityGroup) =>
        createAuthRequest(id, enrolments, affinityGroup, request, block)
        //block(AuthenticatedRequest(request, id.toString, PsaId(getPsaId(enrolments)), userType(affinityGroup)))
      case _ =>
        Future.successful(Redirect(routes.UnauthorisedController.onPageLoad()))
    } recover {
      case _: NoActiveSession =>
        Redirect(config.loginUrl, Map("continue" -> Seq(config.loginContinueUrl)))
      case _: InsufficientEnrolments =>
        Redirect(routes.UnauthorisedController.onPageLoad())
      case _: InsufficientConfidenceLevel =>
        Redirect(routes.UnauthorisedController.onPageLoad())
      case _: UnsupportedAuthProvider =>
        Redirect(routes.UnauthorisedController.onPageLoad())
      case _: UnsupportedAffinityGroup =>
        Redirect(routes.UnauthorisedController.onPageLoad())
      case _: UnsupportedCredentialRole =>
        Redirect(routes.UnauthorisedController.onPageLoad())
      case _: PsaIdNotFound =>
        Redirect(controllers.routes.YouNeedToRegisterController.onPageLoad())
    }
  }

  private def createAuthRequest[A](id: String, enrolments: Enrolments, affinityGroup: AffinityGroup, request: Request[A],
    block: AuthenticatedRequest[A] => Future[Result]): Future[Result] =
    if(authEntity == PSA) {
      block(AuthenticatedRequest(request, id, Some(PsaId(getPsaId(enrolments))), None, userType(affinityGroup)))
    } else {
      block(AuthenticatedRequest(request, id, None, Some(PspId(getPspId(enrolments))), userType(affinityGroup)))
    }


  private def getPsaId(enrolments: Enrolments): String =
    enrolments.getEnrolment("HMRC-PODS-ORG").flatMap(_.getIdentifier("PSAID")).map(_.value)
      .getOrElse(throw new IdNotFound)

  private def getPspId(enrolments: Enrolments): String =
    enrolments.getEnrolment("HMRC-PODS-ORG").flatMap(_.getIdentifier("PSPID")).map(_.value)
      .getOrElse(throw IdNotFound("PspIdNotFound"))

  //
  //private def getPspId(enrolments: Enrolments): String =
  //  enrolments.getEnrolment("HMRC-PODS-ORG").flatMap(_.getIdentifier("PSPID")).map(_.value)
  //
  //private def getPsaId(enrolments: Enrolments) =
  //  enrolments.getEnrolment("HMRC-PODS-ORG").flatMap(_.getIdentifier("PSAID")).map(_.value).getOrElse(throw new PsaIdNotFound)

  private def userType(affinityGroup: AffinityGroup): UserType = {
    affinityGroup match {
      case Individual => models.Individual
      case Organisation => models.Organization
      case _ => OtherUser
    }
  }

}

@ImplementedBy(classOf[AuthImpl])
trait Auth extends ActionBuilder[AuthenticatedRequest, AnyContent] with ActionFunction[Request,
  AuthenticatedRequest]

case class PsaIdNotFound(msg: String = "PsaIdNotFound") extends AuthorisationException(msg)
case class IdNotFound(msg: String = "PsaIdNotFound") extends AuthorisationException(msg)

class AuthActionImpl @Inject()(authConnector: AuthConnector,
  config: FrontendAppConfig,
  val parser: BodyParsers.Default)
  (implicit ec: ExecutionContext) extends AuthAction {

  override def apply(authEntity: AuthEntity): Auth = new AuthImpl(authConnector, config, parser, authEntity)
}

@ImplementedBy(classOf[AuthActionImpl])
trait AuthAction {
  def apply(authEntity: AuthEntity = PSA): Auth
}

//@ImplementedBy(classOf[AuthActionImpl])
//trait AuthAction extends ActionBuilder[AuthenticatedRequest, AnyContent] with ActionFunction[Request, AuthenticatedRequest]
//
//case class PsaIdNotFound(msg: String = "PsaIdNotFound") extends AuthorisationException(msg)

