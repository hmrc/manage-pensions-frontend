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

package controllers.invitation

import com.google.inject.Inject
import config.FrontendAppConfig
import connectors.DataCacheConnector
import controllers.actions.{DataRequiredAction, DataRetrievalAction, AuthAction}
import forms.invitation.PsaIdFromProvider
import identifiers.PSAId
import models.{Mode, NormalMode}
import play.api.i18n.{MessagesApi, I18nSupport}
import play.api.mvc.{Results, AnyContent, Action}
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import utils.{UserAnswers, Navigator}
import utils.annotations.Invitation
import views.html.invitation.psaId
import scala.concurrent.Future


class PsaIdController@Inject()(config:FrontendAppConfig,
                               override val messagesApi: MessagesApi,
                               authenticate: AuthAction,
                               dataCacheConnector: DataCacheConnector,
                               getData: DataRetrievalAction,
                               requireData: DataRequiredAction,
                               formProvider : PsaIdFromProvider) extends BaseController with I18nSupport {

  def onPageLoad(mode:Mode) : Action[AnyContent] = (authenticate andThen getData).async{
    implicit request =>

      val userAnswers = request.userAnswers.getOrElse(new UserAnswers)
      val form = if(userAnswers.get(PSAId).isDefined) {
        formProvider().fill(userAnswers.get(PSAId).get)
      } else {
        formProvider()
      }

      Future.successful(Ok(psaId(config,form, mode)))
  }
}