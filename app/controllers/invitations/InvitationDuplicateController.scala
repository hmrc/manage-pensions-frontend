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
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import identifiers.SchemeDetailId
import identifiers.invitations.PsaNameId
import javax.inject.Inject
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.Navigator
import utils.annotations.Invitation
import views.html.invitations.invitation_duplicate


class InvitationDuplicateController @Inject()(
                                               override val messagesApi: MessagesApi,
                                               frontendAppConfig: FrontendAppConfig,
                                               authenticate: AuthAction,
                                               getData: DataRetrievalAction,
                                               requireData: DataRequiredAction,
                                               @Invitation navigator: Navigator
                                             ) extends FrontendController with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (authenticate andThen getData andThen requireData) {
    implicit request =>


      (for {
        psaName <- request.userAnswers.get(PsaNameId)
        schemeDetail <- request.userAnswers.get(SchemeDetailId)
      } yield {
        Ok(invitation_duplicate(
          frontendAppConfig,
          psaName,
          schemeDetail.schemeName
        ))
      }) getOrElse {
        Redirect(controllers.routes.SessionExpiredController.onPageLoad())
      }
  }
}