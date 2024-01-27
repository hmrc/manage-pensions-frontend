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

package controllers.invitations

import config.FrontendAppConfig
import connectors.UserAnswersCacheConnector
import connectors.admin.MinimalConnector
import controllers.Retrievals
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction, PsaSchemeAuthAction}
import identifiers.MinimalSchemeDetailId
import identifiers.invitations.psa.InviteePSAId
import identifiers.invitations.{InvitationSuccessId, InviteeNameId}
import models.{NormalMode, SchemeReferenceNumber}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.annotations.Invitations
import utils.{Navigator, UserAnswers}
import views.html.invitations.invitation_success

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class InvitationSuccessController @Inject()(
                                             override val messagesApi: MessagesApi,
                                             frontendAppConfig: FrontendAppConfig,
                                             authenticate: AuthAction,
                                             getData: DataRetrievalAction,
                                             requireData: DataRequiredAction,
                                             userAnswersCacheConnector: UserAnswersCacheConnector,
                                             minimalPsaConnector: MinimalConnector,
                                             @Invitations navigator: Navigator,
                                             val controllerComponents: MessagesControllerComponents,
                                             view: invitation_success,
                                             psaSchemeAuthAction: PsaSchemeAuthAction
                                           )(implicit val ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Retrievals {

  def onPageLoad(srn: SchemeReferenceNumber): Action[AnyContent] =
    (authenticate() andThen getData andThen psaSchemeAuthAction(None) andThen requireData).async {
    implicit request =>

      val continue = controllers.invitations.routes.InvitationSuccessController.onSubmit(srn)
      val ua = request.userAnswers

      (ua.get(MinimalSchemeDetailId), ua.get(InviteeNameId), ua.get(InviteePSAId)) match {
        case (Some(schemeDetail), Some(inviteeName), Some(inviteePsaId)) =>
          minimalPsaConnector.getMinimalPsaDetails(inviteePsaId).flatMap { minimalPsaDetails =>
            userAnswersCacheConnector.removeAll(request.externalId).map { _ =>
              Ok(view(
                inviteeName,
                minimalPsaDetails.email,
                schemeDetail.schemeName,
                LocalDate.now().plusDays(frontendAppConfig.invitationExpiryDays),
                continue
              ))
            }
          }
        case _ =>
          Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad))
      }
  }

  def onSubmit(srn: SchemeReferenceNumber): Action[AnyContent] = (authenticate() andThen getData andThen psaSchemeAuthAction(None)).async {
    _ => Future.successful(Redirect(navigator.nextPage(InvitationSuccessId(srn), NormalMode, UserAnswers())))
  }
}
