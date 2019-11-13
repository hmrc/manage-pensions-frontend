/*
 * Copyright 2019 HM Revenue & Customs
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

import javax.inject.Inject
import config.FrontendAppConfig
import controllers.Retrievals
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import identifiers.MinimalSchemeDetailId
import identifiers.invitations.InviteeNameId
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.controller.{FrontendBaseController, FrontendController}
import utils.Navigator
import utils.annotations.Invitation
import views.html.invitations.psa_already_associated

import scala.concurrent.{ExecutionContext, Future}


class PsaAlreadyAssociatedController @Inject()(
                                               override val messagesApi: MessagesApi,
                                               frontendAppConfig: FrontendAppConfig,
                                               authenticate: AuthAction,
                                               getData: DataRetrievalAction,
                                               requireData: DataRequiredAction,
                                               @Invitation navigator: Navigator,
                                               val controllerComponents: MessagesControllerComponents,
                                               view: psa_already_associated
                                             )(implicit val ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Retrievals {

  def onPageLoad(): Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>
      (InviteeNameId and MinimalSchemeDetailId).retrieve.right.map {
        case name ~ schemeDetails =>
          Future.successful(
            Ok(view(
              name,
              schemeDetails.schemeName
            )))
      }
  }
}
