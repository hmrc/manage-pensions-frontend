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

package controllers.invitations

import java.time.LocalDate

import config.FrontendAppConfig
import connectors.UserAnswersCacheConnector
import connectors.admin.MinimalPsaConnector
import controllers.Retrievals
import controllers.actions.AuthAction
import controllers.actions.DataRequiredAction
import controllers.actions.DataRetrievalAction
import identifiers.MinimalSchemeDetailId
import identifiers.invitations.InvitationSuccessId
import identifiers.invitations.InviteeNameId
import identifiers.invitations.InviteePSAId
import javax.inject.Inject
import models.NormalMode
import models.SchemeReferenceNumber
import play.api.i18n.I18nSupport
import play.api.i18n.MessagesApi
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import utils.annotations.Invitation
import utils.Navigator
import utils.UserAnswers
import views.html.invitations.invitation_success

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class InvitationSuccessController @Inject()(
                                             override val messagesApi: MessagesApi,
                                             frontendAppConfig: FrontendAppConfig,
                                             authenticate: AuthAction,
                                             getData: DataRetrievalAction,
                                             requireData: DataRequiredAction,
                                             userAnswersCacheConnector: UserAnswersCacheConnector,
                                             minimalPsaConnector: MinimalPsaConnector,
                                             @Invitation navigator: Navigator,
                                             val controllerComponents: MessagesControllerComponents,
                                             view: invitation_success
                                           )(implicit val ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Retrievals {

  def onPageLoad(srn: SchemeReferenceNumber): Action[AnyContent] = (authenticate() andThen getData andThen requireData).async {
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
          Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad()))
      }
  }

  def onSubmit(srn: SchemeReferenceNumber): Action[AnyContent] = authenticate().async {
    implicit request =>
      Future.successful(Redirect(navigator.nextPage(InvitationSuccessId(srn), NormalMode, UserAnswers())))
  }

}
