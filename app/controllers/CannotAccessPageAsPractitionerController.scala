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
import connectors.UserAnswersCacheConnector
import controllers.actions.AuthAction
import forms.CannotAccessPageAsPractitionerFormProvider
import identifiers.AdministratorOrPractitionerId
import models.AdministratorOrPractitioner
import models.AdministratorOrPractitioner.{Practitioner, Administrator}
import play.api.data.Form
import play.api.i18n.{MessagesApi, Messages, I18nSupport}
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.UserAnswers
import utils.annotations.{SessionDataCache, NoAdministratorOrPractitionerCheck}
import views.html.cannotAccessPageAsPractitioner

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CannotAccessPageAsPractitionerController @Inject()(val appConfig: FrontendAppConfig,
  @NoAdministratorOrPractitionerCheck val auth: AuthAction,
  override val messagesApi: MessagesApi,
  @SessionDataCache cacheConnector: UserAnswersCacheConnector,
  val formProvider: CannotAccessPageAsPractitionerFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: cannotAccessPageAsPractitioner)(implicit
  val ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private object ContinueURLID extends identifiers.TypedIdentifier[String] {
    override def toString: String = "continueURL"
  }

  private def form(implicit messages: Messages): Form[AdministratorOrPractitioner] = formProvider()
  def onPageLoad: Action[AnyContent] = auth().async {
    implicit request =>
      request.request.queryString.find(_._1=="continue").flatMap(_._2.headOption) match {
        case Some(continueUrl) =>
          cacheConnector.save(request.externalId, ContinueURLID, continueUrl).map { _ =>
            Ok(view(form))
          }
        case _ => Future.successful(Redirect(controllers.routes.PspDashboardController.onPageLoad()))
      }
  }

  private def sessionExpired:Result = Redirect(controllers.routes.SessionExpiredController.onPageLoad())

  def onSubmit: Action[AnyContent] = auth().async {
    implicit request =>
      form.bindFromRequest().fold(
        (formWithErrors: Form[_]) =>
          Future.successful(BadRequest(view(formWithErrors))),
        value => {
          cacheConnector.fetch(request.externalId).flatMap { optionJsValue =>
            val optionUA = optionJsValue.map(UserAnswers)
            val optionContinueUrl = optionUA.flatMap(_.get(ContinueURLID))
            val optionUAContinueURLRemoved = optionUA.flatMap(_.remove(ContinueURLID).asOpt)

            val (finalOptionUA, finalResult) = (value, optionContinueUrl) match {
              case (Practitioner, _) =>
                (optionUAContinueURLRemoved, Redirect(controllers.routes.PspDashboardController.onPageLoad()))
              case (Administrator, Some(url)) =>
                optionUAContinueURLRemoved
                  .flatMap(_.set(AdministratorOrPractitionerId)(Administrator).asOpt)
                  .fold(Tuple2(optionUAContinueURLRemoved, sessionExpired))( ua => Tuple2(Some(ua), Redirect(Call("GET", url))))
              case (Administrator, None) => Tuple2(None, sessionExpired)
            }

            finalOptionUA
              .fold(Future.successful(()))(finalUA=> cacheConnector.upsert(request.externalId, finalUA.json).map { _ => ()})
              .map( _ => finalResult)
          }
        }
      )
  }
}
