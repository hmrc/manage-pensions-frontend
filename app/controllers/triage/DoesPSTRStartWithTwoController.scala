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

import forms.triage.DoesPSTRStartWithTwoFormProvider
import identifiers.triage.{DoesPSTRStartWithTwoId, WhatRoleId}
import models.triage.{DoesPSTRStartWithATwo, WhatRole}
import play.api.mvc.Call
import views.html.triage.doesPSTRStartWithTwo
import config.FrontendAppConfig
import controllers.actions.TriageAction

import javax.inject.Inject
import models.NormalMode
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.i18n.Messages
import play.api.i18n.MessagesApi
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.annotations.Triage
import utils.Enumerable
import utils.Navigator
import utils.UserAnswers

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class DoesPSTRStartWithTwoController @Inject()(
                                                appConfig: FrontendAppConfig,
                                                override val messagesApi: MessagesApi,
                                                @Triage navigator: Navigator,
                                                triageAction: TriageAction,
                                                formProvider: DoesPSTRStartWithTwoFormProvider,
                                                val controllerComponents: MessagesControllerComponents,
                                                view: doesPSTRStartWithTwo
                                              )(implicit val ec: ExecutionContext) extends FrontendBaseController with Enumerable.Implicits with I18nSupport {

  private def form(implicit messages: Messages): Form[DoesPSTRStartWithATwo] = formProvider()
  private def postCall(role: String): Call = controllers.triage.routes.DoesPSTRStartWithTwoController.onSubmit(role)

  def onPageLoad(role: String): Action[AnyContent] = triageAction.async {
    implicit request =>
      Future.successful(Ok(view(form, postCall(role))))
  }

  def onSubmit(role: String): Action[AnyContent] = triageAction.async {
    implicit request =>
      form.bindFromRequest().fold(
        (formWithErrors: Form[_]) =>
          Future.successful(BadRequest(view(formWithErrors, postCall(role)))),
        value => {
          val uaUpdated = UserAnswers().set(WhatRoleId)(WhatRole.fromString(role))
            .flatMap(_.set(DoesPSTRStartWithTwoId)(value)).asOpt.getOrElse(UserAnswers())
          Future.successful(Redirect(navigator.nextPage(DoesPSTRStartWithTwoId, NormalMode, uaUpdated)))
        }
      )
  }
}
