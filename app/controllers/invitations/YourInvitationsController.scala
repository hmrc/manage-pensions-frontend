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

package controllers.invitations

import config.FrontendAppConfig
import connectors.{InvitationsCacheConnector, UserAnswersCacheConnector}
import controllers.actions._
import identifiers.SchemeSrnId
import javax.inject.Inject
import models.NormalMode
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.annotations.Invitation
import utils.{Navigator, UserAnswers}
import views.html.invitations.yourInvitations

class YourInvitationsController @Inject()(appConfig: FrontendAppConfig,
                                  override val messagesApi: MessagesApi,
                                  authenticate: AuthAction,
                                  invitationsCacheConnector: InvitationsCacheConnector,
                                  userAnswersCacheConnector: UserAnswersCacheConnector,
                                  @Invitation navigator: Navigator
                                 ) extends FrontendController with I18nSupport {


  def onPageLoad(): Action[AnyContent] = authenticate.async {
    implicit request =>
      invitationsCacheConnector.getForInvitee(request.psaId).map {
        case Nil => Redirect(controllers.routes.SessionExpiredController.onPageLoad())
        case invitationsList => Ok(yourInvitations(appConfig, invitationsList))
      }
  }

  def onSubmit(srn: String): Action[AnyContent] = authenticate.async {
    implicit request =>
      userAnswersCacheConnector.save(request.externalId, SchemeSrnId, srn).map { cacheMap =>
        Redirect(navigator.nextPage(SchemeSrnId, NormalMode, UserAnswers(cacheMap)))
      }
  }




}
