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

package controllers

import config.FrontendAppConfig
import controllers.actions.AuthAction
import forms.CannotAccessPageAsAdministratorFormProvider
import models.AdministratorOrPractitioner
import models.AdministratorOrPractitioner.{Practitioner, Administrator}
import play.api.data.Form
import play.api.i18n.{MessagesApi, Messages, I18nSupport}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.annotations.NoAdministratorOrPractitionerCheck
import views.html.cannotAccessPageAsAdministrator

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CannotAccessPageAsAdministratorController @Inject()(val appConfig: FrontendAppConfig,
                                                          @NoAdministratorOrPractitionerCheck val auth: AuthAction,
                                                          override val messagesApi: MessagesApi,
                                                          val formProvider: CannotAccessPageAsAdministratorFormProvider,
                                                          val controllerComponents: MessagesControllerComponents,
                                                          view: cannotAccessPageAsAdministrator)(implicit
  val ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private def form(implicit messages: Messages): Form[AdministratorOrPractitioner] = formProvider()
  def onPageLoad: Action[AnyContent] = auth().async {
    implicit request =>
      request.request.queryString.find(_._1=="continue").flatMap(_._2.headOption) match {
        case Some(continueUrl) =>
          Future.successful(Ok(view(form)))
        case _ => Future.successful(Redirect(controllers.routes.SchemesOverviewController.onPageLoad()))
      }
  }
  def onSubmit: Action[AnyContent] = auth().async {
    implicit request =>
      form.bindFromRequest().fold(
        (formWithErrors: Form[_]) =>
          Future.successful(BadRequest(view(formWithErrors))),
        { case Administrator =>
            Future.successful(Redirect(controllers.routes.SchemesOverviewController.onPageLoad()))
          case Practitioner =>
            Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad())) // continue to url
        }
      )
  }
}
