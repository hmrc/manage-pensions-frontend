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

package controllers.remove

import com.google.inject.Inject
import config.FrontendAppConfig
import controllers.Retrievals
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import identifiers.invitations.{PSANameId, SchemeNameId}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import views.html.remove.confirmRemoved

import scala.concurrent.Future

class ConfirmRemovedController @Inject()(
  config: FrontendAppConfig,
  override val messagesApi: MessagesApi,
  authenticate: AuthAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction
) extends FrontendController with I18nSupport with Retrievals {

  def onPageLoad(): Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>

      (PSANameId and SchemeNameId).retrieve.right.map {
        case psaName ~ schemeName =>
          Future.successful(Ok(confirmRemoved(config, psaName, schemeName)))
      }

  }

}
