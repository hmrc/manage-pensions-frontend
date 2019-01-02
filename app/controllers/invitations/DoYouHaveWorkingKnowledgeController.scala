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

import config.FrontendAppConfig
import connectors.UserAnswersCacheConnector
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import forms.invitations.DoYouHaveWorkingKnowledgeFormProvider
import identifiers.invitations.DoYouHaveWorkingKnowledgeId
import javax.inject.Inject
import models.Mode
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.annotations.AcceptInvitation
import utils.{Navigator, UserAnswers}
import views.html.invitations.doYouHaveWorkingKnowledge

import scala.concurrent.{ExecutionContext, Future}

class DoYouHaveWorkingKnowledgeController @Inject()(
                                                     val appConfig: FrontendAppConfig,
                                                     val auth: AuthAction,
                                                     val messagesApi: MessagesApi,
                                                     @AcceptInvitation navigator: Navigator,
                                                     val formProvider: DoYouHaveWorkingKnowledgeFormProvider,
                                                     val dataCacheConnector: UserAnswersCacheConnector,
                                                     val getData: DataRetrievalAction,
                                                     val requireData: DataRequiredAction
                                                       )(implicit val ec: ExecutionContext) extends FrontendController with I18nSupport {

  val form: Form[Boolean] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (auth andThen getData andThen requireData) {
    implicit request =>

      val preparedForm = request.userAnswers.get(DoYouHaveWorkingKnowledgeId) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(doYouHaveWorkingKnowledge(appConfig, preparedForm, mode))

  }

  def onSubmit(mode: Mode): Action[AnyContent] = auth.async {
    implicit request =>
      form.bindFromRequest().fold(
        (formWithErrors: Form[Boolean]) =>
          Future.successful(BadRequest(doYouHaveWorkingKnowledge(appConfig, formWithErrors, mode))),
        value => {
          dataCacheConnector.save(request.externalId, DoYouHaveWorkingKnowledgeId, value).map(
            cacheMap =>
              Redirect(navigator.nextPage(DoYouHaveWorkingKnowledgeId, mode, UserAnswers(cacheMap)))
          )
        }
      )
  }
}
