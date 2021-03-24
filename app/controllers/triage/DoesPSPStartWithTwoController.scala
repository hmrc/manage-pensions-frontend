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

package controllers.triage

import controllers.actions.TriageAction
import forms.triage.DoesPSTRStartWithTwoFormProvider
import identifiers.triage.DoesPSPStartWithTwoId
import models.NormalMode
import models.triage.DoesPSTRStartWithATwo
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.annotations.Triage
import utils.{Enumerable, Navigator, UserAnswers}
import views.html.triage.doesPspStartWithTwo

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DoesPSPStartWithTwoController @Inject()(
                                                      override val messagesApi: MessagesApi,
                                                      @Triage navigator: Navigator,
                                                      triageAction: TriageAction,
                                                      formProvider: DoesPSTRStartWithTwoFormProvider,
                                                      val controllerComponents: MessagesControllerComponents,
                                                      view: doesPspStartWithTwo
                                                    )(implicit val ec: ExecutionContext)
  extends FrontendBaseController
    with Enumerable.Implicits
    with I18nSupport {

  private def form(implicit messages: Messages): Form[DoesPSTRStartWithATwo] = formProvider("messages__doesPSPStartWithTwo__error")
  private def postCall: Call = controllers.triage.routes.DoesPSPStartWithTwoController.onSubmit()

  def onPageLoad: Action[AnyContent] = triageAction.async {
    implicit request =>

      Future.successful(Ok(view(form, postCall)))
  }

  def onSubmit: Action[AnyContent] = triageAction.async {
    implicit request =>
      form.bindFromRequest().fold(
        (formWithErrors: Form[_]) =>
          Future.successful(BadRequest(view(formWithErrors, postCall))),
        value => {
          val uaUpdated = UserAnswers().set(DoesPSPStartWithTwoId)(value).asOpt.getOrElse(UserAnswers())
          Future.successful(Redirect(navigator.nextPage(DoesPSPStartWithTwoId, NormalMode, uaUpdated)))
        }
      )
  }
}
