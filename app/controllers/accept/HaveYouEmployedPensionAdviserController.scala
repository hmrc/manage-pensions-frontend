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

package controllers.accept

import javax.inject.Inject

import config.FrontendAppConfig
import controllers.actions.AuthAction
import forms.accept.HaveYouEmployedPensionAdviserFormProvider
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import views.html.accept.haveYouEmployedPensionAdviser
import connectors.DataCacheConnector
import identifiers.accept.EmployedPensionAdviserId
import models.Mode
import utils.{Navigator, UserAnswers}
import utils.annotations.AcceptInvitation

import scala.concurrent.Future

class HaveYouEmployedPensionAdviserController @Inject()(
                                                         val appConfig: FrontendAppConfig,
                                                         val auth: AuthAction,
                                                         val messagesApi: MessagesApi,
                                                         @AcceptInvitation navigator: Navigator,
                                                         val formProvider: HaveYouEmployedPensionAdviserFormProvider,
                                                         val dataCacheConnector: DataCacheConnector
                                                       ) extends FrontendController with I18nSupport {

  val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = auth {
    implicit request =>
      Ok(haveYouEmployedPensionAdviser(appConfig, formProvider(), mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = auth.async {
    implicit request =>
      form.bindFromRequest().fold(
        (formWithErrors: Form[Boolean]) =>
          Future.successful(BadRequest(haveYouEmployedPensionAdviser(appConfig, formWithErrors, mode))),
        (value) => {
          dataCacheConnector.save(request.externalId, EmployedPensionAdviserId, value).map(
            cacheMap =>
              Redirect(navigator.nextPage(EmployedPensionAdviserId, mode, UserAnswers(cacheMap)))
          )
        }
      )
  }
}