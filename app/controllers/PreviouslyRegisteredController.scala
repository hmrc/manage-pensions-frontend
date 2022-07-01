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
import models.PreviouslyRegistered.PreviouslyRegisteredButNotLoggedIn
import models.{AdministratorOrPractitioner, PreviouslyRegistered}
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.previouslyRegistered

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class PreviouslyRegisteredController @Inject()(
                                                val appConfig: FrontendAppConfig,
                                                override val messagesApi: MessagesApi,
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
          case PreviouslyRegisteredButNotLoggedIn => Redirect(appConfig.recoverCredentialsPSAUrl)
          case _ => Redirect(appConfig.registerSchemeAdministratorUrl)
        }
      )
  }

  def onSubmitPractitioner: Action[AnyContent] = Action {
    implicit request =>
      form.bindFromRequest().fold(
        (formWithErrors: Form[_]) =>
          BadRequest(view(formWithErrors, AdministratorOrPractitioner.Practitioner)),
        {
          case PreviouslyRegisteredButNotLoggedIn => Redirect(appConfig.recoverCredentialsPSPUrl)
          case _ => Redirect(appConfig.registerSchemePractitionerUrl)
        }
      )
  }
}
