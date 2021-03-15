/*
 * Copyright 2021 HM Revenue & Customs
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
import connectors.UserAnswersCacheConnector
import controllers.routes
import identifiers.AdministratorOrPractitionerId
import models.AdministratorOrPractitioner.{Practitioner, Administrator}
import models.AuthEntity
import models.AuthEntity.{PSA, PSP}
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
import utils.UserAnswers

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class AuthImpl(
                override val authConnector: AuthConnector,
                userAnswersCacheConnector: UserAnswersCacheConnector,
                config: FrontendAppConfig,
                val parser: BodyParsers.Default,
                authEntity: AuthEntity,
                administratorOrPractitionerCheck: Boolean
              )(implicit val executionContext: ExecutionContext)
  extends Auth
    with AuthorisedFunctions {

  override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

    authorised().retrieve(
      Retrievals.externalId and Retrievals.allEnrolments and Retrievals.affinityGroup
    ) {
      case Some(id) ~ enrolments ~ Some(affinityGroup) =>
        createAuthRequest(id, enrolments, affinityGroup, request, block)
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
      case _: IdNotFound =>
        Redirect(controllers.routes.YouNeedToRegisterController.onPageLoad())
    }
  }

  private def createAuthRequest[A](
    id: String,
    enrolments: Enrolments,
    affinityGroup: AffinityGroup,
    request: Request[A],
    block: AuthenticatedRequest[A] => Future[Result]
  ): Future[Result] = {

    val (psaId, pspId) = if (authEntity == PSA) {
      (getPsaId(isMandatory = true, enrolments), getPspId(isMandatory = false, enrolments))
    } else {
      (getPsaId(isMandatory = false, enrolments), getPspId(isMandatory = true, enrolments))
    }

    (psaId, pspId) match {
      case (Some(_), Some(_)) if administratorOrPractitionerCheck => handleWhereBothEnrolments(id, enrolments, affinityGroup, request, block)
      case _ => block(AuthenticatedRequest(request, id, psaId, pspId, userType(affinityGroup)))
    }
  }

  private def handleWhereBothEnrolments[A](
    id: String,
    enrolments: Enrolments,
    affinityGroup: AffinityGroup,
    request: Request[A],
    block: AuthenticatedRequest[A] => Future[Result]
  ):Future[Result] = {
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))
      userAnswersCacheConnector.fetch(id).flatMap { optionJsValue =>
        optionJsValue.map(UserAnswers).flatMap(_.get(AdministratorOrPractitionerId)) match {
          case None => Future.successful(Redirect(controllers.routes.AdministratorOrPractitionerController.onPageLoad()))
          case Some(aop) =>
            val (psaId:Option[PsaId], pspId:Option[PspId]) = (aop, authEntity) match {
              case (Administrator, PSA) => (getPsaId(isMandatory = true, enrolments), getPspId(isMandatory = false, enrolments))
              case (Practitioner, PSP) => (getPsaId(isMandatory = false, enrolments), getPspId(isMandatory = true, enrolments))
              case (Administrator, PSP) => Future.successful(Redirect(controllers.routes.InterruptToAdministratorController.onPageLoad())) // TODO: Redirect to new page
              case (Practitioner, PSA) => Future.successful(Redirect(controllers.routes.InterruptToPractitionerController.onPageLoad())) // TODO: Redirect to new page
            }
            block(AuthenticatedRequest(request, id, psaId, pspId, userType(affinityGroup)))
        }
      }
  }

  private def getPsaId(isMandatory: Boolean, enrolments: Enrolments): Option[PsaId] = {
    def failureResult: Option[PsaId] = if (isMandatory) throw IdNotFound() else None
    enrolments.getEnrolment("HMRC-PODS-ORG")
      .flatMap(_.getIdentifier("PSAID")).map(_.value)
      .fold[Option[PsaId]](failureResult)(id => Some(PsaId(id)))
  }

  private def getPspId(isMandatory: Boolean, enrolments: Enrolments): Option[PspId] = {
    def failureResult: Option[PspId] = if (isMandatory) throw IdNotFound("PspIdNotFound") else None

    enrolments.getEnrolment("HMRC-PODSPP-ORG")
      .flatMap(_.getIdentifier("PSPID")).map(_.value)
      .fold[Option[PspId]](failureResult)(id => Some(PspId(id)))
  }

  private def userType(affinityGroup: AffinityGroup): UserType = {
    affinityGroup match {
      case Individual => models.Individual
      case Organisation => models.Organization
      case _ => OtherUser
    }
  }

}

@ImplementedBy(classOf[AuthImpl])
trait Auth
  extends ActionBuilder[AuthenticatedRequest, AnyContent]
    with ActionFunction[Request, AuthenticatedRequest]

case class IdNotFound(msg: String = "PsaIdNotFound") extends AuthorisationException(msg)

class AuthActionImpl @Inject()(
                                authConnector: AuthConnector,
                                userAnswersCacheConnector: UserAnswersCacheConnector,
                                config: FrontendAppConfig,
                                val parser: BodyParsers.Default
                              )(implicit ec: ExecutionContext)
  extends AuthAction {

  override def apply(authEntity: AuthEntity): Auth =
    new AuthImpl(authConnector, userAnswersCacheConnector, config, parser, authEntity, administratorOrPractitionerCheck = true)
}

class AuthActionNoAdministratorOrPractitionerCheckImpl @Inject()(
  authConnector: AuthConnector,
  userAnswersCacheConnector: UserAnswersCacheConnector,
  config: FrontendAppConfig,
  val parser: BodyParsers.Default
)(implicit ec: ExecutionContext)
  extends AuthAction {

  override def apply(authEntity: AuthEntity): Auth =
    new AuthImpl(authConnector, userAnswersCacheConnector, config, parser, authEntity, administratorOrPractitionerCheck = false)
}

trait AuthAction {
  def apply(authEntity: AuthEntity = PSA): Auth
}
