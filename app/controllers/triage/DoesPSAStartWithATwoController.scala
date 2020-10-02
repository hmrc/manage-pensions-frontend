/*
 * Copyright 2020 HM Revenue & Customs
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

package controllers.triage

import config.FrontendAppConfig
import controllers.Retrievals
import controllers.actions.TriageAction
import forms.triage.DoesPSAStartWithATwoFormProvider
import identifiers.triage.DoesPSAStartWithATwoId
import javax.inject.Inject
import models.NormalMode
import models.triage.DoesPSAStartWithATwo
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.i18n.Messages
import play.api.i18n.MessagesApi
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import utils.annotations.Triage
import utils.Enumerable
import utils.Navigator
import utils.UserAnswers
import views.html.triage.doesPSAStartWithATwo

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class DoesPSAStartWithATwoController @Inject()(appConfig: FrontendAppConfig,
                                               override val messagesApi: MessagesApi,
                                               @Triage navigator: Navigator,
                                               triageAction: TriageAction,
                                               formProvider: DoesPSAStartWithATwoFormProvider,
                                               val controllerComponents: MessagesControllerComponents,
                                               val view: doesPSAStartWithATwo
                                              )(implicit val executionContext: ExecutionContext
                                              ) extends FrontendBaseController with I18nSupport with Enumerable.Implicits with Retrievals {

  private def form(implicit messages: Messages): Form[DoesPSAStartWithATwo] = formProvider()

  def onPageLoad: Action[AnyContent] = triageAction.async {
    implicit request =>
      Future.successful(Ok(view(form)))
  }

  def onSubmit: Action[AnyContent] = triageAction.async {
    implicit request =>
      form.bindFromRequest().fold(
        (formWithErrors: Form[_]) =>
          Future.successful(BadRequest(view(formWithErrors))),
        value => {
          val uaUpdated = UserAnswers().set(DoesPSAStartWithATwoId)(value).asOpt.getOrElse(UserAnswers())
          Future.successful(Redirect(navigator.nextPage(DoesPSAStartWithATwoId, NormalMode, uaUpdated)))
        }
      )
  }
}
