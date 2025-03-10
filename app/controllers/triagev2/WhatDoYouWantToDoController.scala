/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.triagev2

import connectors.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions.{DataRequiredAction, DataRetrievalAction, TriageAction, TriageToAuthAction}
import forms.triagev2.WhatDoYouWantToDoFormProvider
import identifiers.triagev2.{WhatDoYouWantToDoId, WhatRoleId}
import models.NormalMode
import models.triagev2.{WhatDoYouWantToDo, WhatRole}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.annotations.TriageV2
import utils.{Enumerable, Navigator, UserAnswers}
import views.html.triagev2.whatDoYouWantToDo

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class WhatDoYouWantToDoController @Inject()(override val messagesApi: MessagesApi,
                                            @TriageV2 navigator: Navigator,
                                            triageAction: TriageAction,
                                            triageToAuthAction: TriageToAuthAction,
                                            getData: DataRetrievalAction,
                                            requireData: DataRequiredAction,
                                            formProvider: WhatDoYouWantToDoFormProvider,
                                            userAnswersCacheConnector: UserAnswersCacheConnector,
                                            val controllerComponents: MessagesControllerComponents,
                                            val view: whatDoYouWantToDo
                                           )(implicit val executionContext: ExecutionContext
                                           ) extends FrontendBaseController with I18nSupport with Enumerable.Implicits with Retrievals {

  private def form(role: String): Form[WhatDoYouWantToDo] = formProvider(role)

  def onPageLoad(role: String): Action[AnyContent] = (triageAction andThen triageToAuthAction andThen getData andThen requireData).async {
    implicit request =>
      val formInstance = form(role)
      val preparedForm = request.userAnswers.get(WhatDoYouWantToDoId) match {
        case None => formInstance
        case Some(value) => formInstance.fill(value)
      }
      Future.successful(Ok(view(preparedForm, role)))
  }

  def onSubmit(role: String): Action[AnyContent] = (triageAction andThen triageToAuthAction andThen getData andThen requireData).async {
    implicit request =>
      form(role).bindFromRequest().fold(
        (formWithErrors: Form[_]) =>
          Future.successful(BadRequest(view(formWithErrors, role))),
        value => {
          for {
            _ <- userAnswersCacheConnector.save(request.externalId, WhatRoleId, WhatRole.fromString(role))
            cacheMap <- userAnswersCacheConnector.save(request.externalId, WhatDoYouWantToDoId, value)
          } yield Redirect(navigator.nextPage(WhatDoYouWantToDoId, NormalMode, UserAnswers(cacheMap)))
        }
      )
  }
}