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

package controllers.invitations.psp

import config.FrontendAppConfig
import connectors.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions.AuthAction
import controllers.actions.DataRequiredAction
import controllers.actions.DataRetrievalAction
import identifiers.SchemeNameId
import identifiers.invitations.psp.PspNameId
import javax.inject.Inject
import play.api.i18n.I18nSupport
import play.api.i18n.MessagesApi
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.invitations.psp.confirmation

import scala.concurrent.ExecutionContext

class ConfirmationController @Inject()(
                                             override val messagesApi: MessagesApi,
                                             frontendAppConfig: FrontendAppConfig,
                                             authenticate: AuthAction,
                                             getData: DataRetrievalAction,
                                             requireData: DataRequiredAction,
                                             userAnswersCacheConnector: UserAnswersCacheConnector,
                                             val controllerComponents: MessagesControllerComponents,
                                             view: confirmation
                                           )(implicit val ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Retrievals {

  def onPageLoad: Action[AnyContent] = (authenticate() andThen getData andThen requireData).async {
    implicit request =>
      (SchemeNameId and PspNameId).retrieve.right.map {
        case schemeName ~ pspName =>
          userAnswersCacheConnector.removeAll(request.externalId).map { _ =>
            Ok(view(schemeName, pspName))
          }
      }
  }
}
