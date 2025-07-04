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

package controllers

import config.FrontendAppConfig
import connectors.UserAnswersCacheConnector
import controllers.actions.{DataRetrievalAction, DataRequiredAction, AuthAction}
import forms.AdministratorOrPractitionerFormProvider
import identifiers.AdministratorOrPractitionerId

import javax.inject.Inject
import models.{AdministratorOrPractitioner, NormalMode}
import play.api.data.Form
import play.api.i18n.{MessagesApi, Messages, I18nSupport}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.annotations.{NoAdministratorOrPractitionerCheck, SessionDataCache}
import utils.{UserAnswers, Navigator}
import views.html.administratorOrPractitioner

import scala.concurrent.{ExecutionContext, Future}

class AdministratorOrPractitionerController @Inject()(
                                                     val appConfig: FrontendAppConfig,
                                                     @NoAdministratorOrPractitionerCheck val auth: AuthAction,
                                                     override val messagesApi: MessagesApi,
                                                     navigator: Navigator,
                                                     val formProvider: AdministratorOrPractitionerFormProvider,
                                                     @SessionDataCache val dataCacheConnector: UserAnswersCacheConnector,
                                                     val getData: DataRetrievalAction,
                                                     val requireData: DataRequiredAction,
                                                     val controllerComponents: MessagesControllerComponents,
                                                     view: administratorOrPractitioner
                                                       )(implicit val ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private def form(implicit messages: Messages): Form[AdministratorOrPractitioner] = formProvider()

  def onPageLoad: Action[AnyContent] = auth().async {
    implicit request =>
      Future.successful(Ok(view(form)))
  }

  def onSubmit: Action[AnyContent] = auth().async {
    implicit request =>
      form.bindFromRequest().fold(
        (formWithErrors: Form[?]) =>
          Future.successful(BadRequest(view(formWithErrors))),
        value => {
          dataCacheConnector.save(request.externalId, AdministratorOrPractitionerId, value).map(
            json =>
              Redirect(navigator.nextPage(AdministratorOrPractitionerId, NormalMode, UserAnswers(json)))
          )
        }
      )
  }
}
