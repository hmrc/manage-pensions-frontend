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

package controllers.actions

import com.google.inject.{ImplementedBy, Inject}
import config.FrontendAppConfig
import connectors.UserAnswersCacheConnector
import controllers.psa.routes._
import controllers.psp.routes._
import controllers.routes._
import identifiers.AdministratorOrPractitionerId
import models.AdministratorOrPractitioner.{Administrator, Practitioner}
import models.AuthEntity.{PSA, PSP}
import models.requests.AuthenticatedRequest
import models.{AuthEntity, OtherUser, UserType}
import play.api.Logging
import play.api.mvc.Results._
import play.api.mvc._
import uk.gov.hmrc.auth.core.AffinityGroup.{Individual, Organisation}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.domain.{PsaId, PspId}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.UserAnswers
import utils.annotations.SessionDataCache

import scala.concurrent.{ExecutionContext, Future}

class AuthImpl(
                override val authConnector: AuthConnector,
                sessionDataCacheConnector: UserAnswersCacheConnector,
                config: FrontendAppConfig,
                val parser: BodyParsers.Default,
                authEntity: AuthEntity,
                administratorOrPractitionerCheck: Boolean
              )(implicit val executionContext: ExecutionContext)
  extends Auth
    with AuthorisedFunctions with Logging {

  override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    authorised().retrieve(
      Retrievals.externalId and Retrievals.allEnrolments and Retrievals.affinityGroup
    ) {
      case Some(id) ~ enrolments ~ Some(affinityGroup) =>
        createAuthRequest(id, enrolments, affinityGroup, request, block)
      case _ =>
        Future.successful(Redirect(UnauthorisedController.onPageLoad))
    } recover {
      case _: NoActiveSession =>
        Redirect(config.loginUrl, Map("continue" -> Seq(config.loginContinueUrl)))
      case _: InsufficientEnrolments =>
        Redirect(UnauthorisedController.onPageLoad)
      case _: InsufficientConfidenceLevel =>
        Redirect(UnauthorisedController.onPageLoad)
      case _: UnsupportedAuthProvider =>
        Redirect(UnauthorisedController.onPageLoad)
      case _: UnsupportedAffinityGroup =>
        Redirect(UnauthorisedController.onPageLoad)
      case _: UnsupportedCredentialRole =>
        Redirect(UnauthorisedController.onPageLoad)
      case _: IdNotFound =>
        Redirect(YouNeedToRegisterController.onPageLoad())
    }
  }

  // scalastyle:off
  private def createAuthRequest[A](
                                    id: String,
                                    enrolments: Enrolments,
                                    affinityGroup: AffinityGroup,
                                    request: Request[A],
                                    block: AuthenticatedRequest[A] => Future[Result]
                                  ): Future[Result] = {

    val (psaId, pspId) = (getPsaId(isMandatory = false, enrolments), getPspId(isMandatory = false, enrolments))

    /**
     * This function handles exceptional case of redirecting to the appropriate dashboard based on user type (PSA/PSP)
     * If PSA is trying to access PSP dashboard should be redirected to PSA dashboard.
     * If PSP is trying to access PSA dashboard should be redirected to PSP dashboard.
     * Many services depend on this interaction.
     * Example: The header link of all MPS services by default redirects to PSA dashboard.
     * @param psaId
     * @param pspId
     * @param request
     * @return A redirect to appropriate dashboard
     */
    def handleDashboardPageRedirect(psaId: Option[PsaId], pspId: Option[PspId], request: Request[A]):Option[Future[Result]] = {
      def psaDashboardCall = controllers.routes.SchemesOverviewController.onPageLoad()
      def pspDashboardCall = controllers.psp.routes.PspDashboardController.onPageLoad()
      psaId -> pspId match {
        case (None, Some(_)) if request.path == psaDashboardCall.url =>
          Some(
            Future.successful(
              Redirect(pspDashboardCall)
            )
          )
        case (Some(_), None) if request.path == pspDashboardCall.url =>
          Some(
            Future.successful(
              Redirect(psaDashboardCall)
            )
          )
        case _ => None
      }
    }

    def handleAuthEntity(authEntity: AuthEntity, psaId: Option[PsaId], pspId: Option[PspId]): Unit = {
      def throwException(msg: String) = throw IdNotFound(msg)
      authEntity match {
        case AuthEntity.PSA if psaId.isEmpty => throwException("PsaIdNotFound")
        case AuthEntity.PSP if pspId.isEmpty => throwException("PspIdNotFound")
        case _ => ()
      }
    }

    handleDashboardPageRedirect(psaId, pspId, request).getOrElse({
      handleAuthEntity(authEntity, psaId, pspId)
      (psaId, pspId) match {
        case (Some(_), Some(_)) if administratorOrPractitionerCheck =>
          handleWhereBothEnrolments(id, enrolments, affinityGroup, request, block)
        case (None, None) =>
          logger.warn("AuthAction, neither enrolment is available")
          Future.successful(Redirect(UnauthorisedController.onPageLoad))
        case _ =>
          block(AuthenticatedRequest(request, id, psaId, pspId, userType(affinityGroup)))
      }
    })
  }

  private def handleWhereBothEnrolments[A](
                                            id: String,
                                            enrolments: Enrolments,
                                            affinityGroup: AffinityGroup,
                                            request: Request[A],
                                            block: AuthenticatedRequest[A] => Future[Result]
                                          ): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    sessionDataCacheConnector.fetch(id).flatMap { optionJsValue =>

      def friendlyUrl: String = config.localFriendlyUrl(request.uri)

      optionJsValue.map(UserAnswers).flatMap(_.get(AdministratorOrPractitionerId)) match {
        case None => Future.successful(Redirect(AdministratorOrPractitionerController.onPageLoad()))
        case Some(aop) =>
          (aop, authEntity) match {
            case (Administrator, PSA) =>
              block(AuthenticatedRequest(request, id,
                getPsaId(isMandatory = true, enrolments), getPspId(isMandatory = false, enrolments), userType(affinityGroup)))
            case (Practitioner, PSP) =>
              block(AuthenticatedRequest(request, id,
                getPsaId(isMandatory = false, enrolments), getPspId(isMandatory = true, enrolments), userType(affinityGroup)))
            case (Administrator, PSP) =>
              Future.successful(
                Redirect(Call("GET", s"${CannotAccessPageAsAdministratorController.onPageLoad().url}?continue=$friendlyUrl"))
              )
            case (Practitioner, PSA) =>
              Future.successful(
                Redirect(Call("GET", s"${CannotAccessPageAsPractitionerController.onPageLoad().url}?continue=$friendlyUrl"))
              )
          }
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
                                @SessionDataCache sessionDataCacheConnector: UserAnswersCacheConnector,
                                config: FrontendAppConfig,
                                val parser: BodyParsers.Default
                              )(implicit ec: ExecutionContext)
  extends AuthAction {

  override def apply(authEntity: AuthEntity): Auth =
    new AuthImpl(authConnector, sessionDataCacheConnector, config, parser, authEntity, administratorOrPractitionerCheck = true)
}

class AuthActionNoAdministratorOrPractitionerCheckImpl @Inject()(
                                                                  authConnector: AuthConnector,
                                                                  @SessionDataCache sessionDataCacheConnector: UserAnswersCacheConnector,
                                                                  config: FrontendAppConfig,
                                                                  val parser: BodyParsers.Default
                                                                )(implicit ec: ExecutionContext)
  extends AuthAction {

  override def apply(authEntity: AuthEntity): Auth =
    new AuthImpl(authConnector, sessionDataCacheConnector, config, parser, authEntity, administratorOrPractitionerCheck = false)
}

trait AuthAction {
  def apply(authEntity: AuthEntity = PSA): Auth
}
