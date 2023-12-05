/*
 * Copyright 2023 HM Revenue & Customs
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
import play.api.mvc._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions, Enrolments}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.previouslyRegistered

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PreviouslyRegisteredController @Inject()(
                                                val appConfig: FrontendAppConfig,
                                                override val authConnector: AuthConnector,
                                                override val messagesApi: MessagesApi,
                                                val formProvider: PreviouslyRegisteredFormProvider,
                                                val controllerComponents: MessagesControllerComponents,
                                                view: previouslyRegistered
                                              )(implicit val ec: ExecutionContext) extends FrontendBaseController with I18nSupport with AuthorisedFunctions {

  private def form(implicit messages: Messages): Form[PreviouslyRegistered] = formProvider()

  def onPageLoadAdministrator: Action[AnyContent] = Action {
    implicit request =>
      Ok(view(form, AdministratorOrPractitioner.Administrator))
  }

  def onPageLoadPractitioner: Action[AnyContent] = Action {
    implicit request =>
      Ok(view(form, AdministratorOrPractitioner.Practitioner))
  }

  def onSubmitAdministrator: Action[AnyContent] = Action.async {
    implicit request =>
      form.bindFromRequest().fold(
        (formWithErrors: Form[_]) =>
          Future.successful(BadRequest(view(formWithErrors, AdministratorOrPractitioner.Administrator))),
        {
          case PreviouslyRegisteredButNotLoggedIn =>
            previouslyRegisteredButNotLoggedIn("HMRC-PSA-ORG", "PSAID", "A0", appConfig.recoverCredentialsPSAUrl)
          case _ => Future.successful(Redirect(appConfig.registerSchemeAdministratorUrl))
        }
      )
  }

  def onSubmitPractitioner: Action[AnyContent] = Action.async {
    implicit request =>
      form.bindFromRequest().fold(
        (formWithErrors: Form[_]) =>
          Future.successful(BadRequest(view(formWithErrors, AdministratorOrPractitioner.Practitioner))),
        {
          case PreviouslyRegisteredButNotLoggedIn =>
            previouslyRegisteredButNotLoggedIn("HMRC-PP-ORG", "PPID", "0", appConfig.recoverCredentialsPSPUrl)
          case _ => Future.successful(Redirect(appConfig.registerSchemePractitionerUrl))
        }
      )
  }

  private def previouslyRegisteredButNotLoggedIn(enrolmentKey: String, idName: String, idStartsWith: String, recoverCredentialsUrl: String)
                                                (implicit messagesRequest: MessagesRequest[AnyContent]): Future[Result] = {
    authorised().retrieve(Retrievals.allEnrolments) {
      enrolments =>
        isTpssAccount(enrolments, enrolmentKey, idName, idStartsWith) match {
          case true => Future.successful(Redirect(routes.TpssRecoveryController.onPageLoad))
          case _ => Future.successful(Redirect(recoverCredentialsUrl))
        }
    }
  }

  private def isTpssAccount(enrolments: Enrolments, enrolmentKey: String, idName: String, idStartsWith: String): Boolean = {
    val psaVal = enrolments.getEnrolment(enrolmentKey).flatMap(_.getIdentifier(idName)).map(_.value)
    psaVal match {
      case Some(tpssVal) if tpssVal.startsWith(idStartsWith) => true
      case _ => false
    }
  }
}
