/*
 * Copyright 2022 HM Revenue & Customs
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
import forms.PreviouslyRegisteredFormProvider
import identifiers.AdministratorOrPractitionerId
import models.PreviouslyRegistered.{YesNotLoggedIn, YesStopped}
import models.{AdministratorOrPractitioner, NormalMode, PreviouslyRegistered}
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.{Navigator, UserAnswers}
import views.html.previouslyRegistered

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class PreviouslyRegisteredController @Inject()(
                                                val appConfig: FrontendAppConfig,
                                                override val messagesApi: MessagesApi,
                                                navigator: Navigator,
                                                val formProvider: PreviouslyRegisteredFormProvider,
                                                val controllerComponents: MessagesControllerComponents,
                                                view: previouslyRegistered
                                              )(implicit val ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private def form(implicit messages: Messages): Form[PreviouslyRegistered] = formProvider()

  def onPageLoadAdministrator: Action[AnyContent] = Action {
    implicit request =>
      Ok(view(form, AdministratorOrPractitioner.Administrator))
  }

  def onPageLoadPractitioner: Action[AnyContent] = Action {
    implicit request =>
      Ok(view(form, AdministratorOrPractitioner.Practitioner))
  }

  def onSubmitAdministrator: Action[AnyContent] = Action {
    implicit request =>
      form.bindFromRequest().fold(
        (formWithErrors: Form[_]) =>
          BadRequest(view(formWithErrors, AdministratorOrPractitioner.Administrator)),
        {
          case YesNotLoggedIn => Redirect(navigator.nextPage(AdministratorOrPractitionerId, NormalMode, UserAnswers()))
          case YesStopped => Redirect(navigator.nextPage(AdministratorOrPractitionerId, NormalMode, UserAnswers()))
          case _ => Redirect(navigator.nextPage(AdministratorOrPractitionerId, NormalMode, UserAnswers()))
        }
      )
  }

  def onSubmitPractitioner: Action[AnyContent] = Action {
    implicit request =>
      form.bindFromRequest().fold(
        (formWithErrors: Form[_]) =>
          BadRequest(view(formWithErrors, AdministratorOrPractitioner.Practitioner)),
        {
          case YesNotLoggedIn => Redirect(navigator.nextPage(AdministratorOrPractitionerId, NormalMode, UserAnswers()))
          case YesStopped => Redirect(navigator.nextPage(AdministratorOrPractitionerId, NormalMode, UserAnswers()))
          case _ => Redirect(navigator.nextPage(AdministratorOrPractitionerId, NormalMode, UserAnswers()))
        }
      )
  }
}
