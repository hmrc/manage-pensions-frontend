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
import forms.triage.WhatDoYouWantToDoFormProvider
import identifiers.triage.WhatDoYouWantToDoId
import javax.inject.Inject
import models.NormalMode
import models.triage.WhatDoYouWantToDo
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import utils.annotations.Triage
import utils.{Enumerable, Navigator, UserAnswers}
import views.html.triage.whatDoYouWantToDo

import scala.concurrent.{ExecutionContext, Future}

class WhatDoYouWantToDoController @Inject()(appConfig: FrontendAppConfig,
                                            override val messagesApi: MessagesApi,
                                            @Triage navigator: Navigator,
                                            triageAction: TriageAction,
                                            formProvider: WhatDoYouWantToDoFormProvider,
                                            val controllerComponents: MessagesControllerComponents,
                                            val view: whatDoYouWantToDo
                                           )(implicit val executionContext: ExecutionContext
                                           ) extends FrontendBaseController with I18nSupport with Enumerable.Implicits with Retrievals {

  private def form: Form[WhatDoYouWantToDo] = formProvider()

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
          val uaUpdated = UserAnswers().set(WhatDoYouWantToDoId)(value).asOpt.getOrElse(UserAnswers())
          Future.successful(Redirect(navigator.nextPage(WhatDoYouWantToDoId, NormalMode, uaUpdated)))
        }
      )
  }
}