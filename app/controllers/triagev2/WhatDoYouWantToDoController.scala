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

import connectors.ManagePensionsCacheConnector
import controllers.Retrievals
import controllers.actions.AuthAction
import forms.triagev2.WhatDoYouWantToDoFormProvider
import identifiers.triagev2.WhatDoYouWantToDoId
import models.NormalMode
import models.triagev2.WhatDoYouWantToDo
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{JsError, JsObject, JsString, JsSuccess}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.annotations.TriageV2
import utils.{Enumerable, Navigator, UserAnswers}
import views.html.triagev2.whatDoYouWantToDo

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class WhatDoYouWantToDoController @Inject()(override val messagesApi: MessagesApi,
                                            @TriageV2 navigator: Navigator,
                                            val auth: AuthAction,
                                            formProvider: WhatDoYouWantToDoFormProvider,
                                            val controllerComponents: MessagesControllerComponents,
                                            val view: whatDoYouWantToDo,
                                            val managePensionsCacheConnector: ManagePensionsCacheConnector
                                           )(implicit val executionContext: ExecutionContext
                                           ) extends FrontendBaseController with I18nSupport with Enumerable.Implicits with Retrievals {

  private def form(role: String): Form[WhatDoYouWantToDo] = formProvider(role)

  def onPageLoad(role: String): Action[AnyContent] = auth().async {
    implicit request =>
      managePensionsCacheConnector.fetch(request.externalId).map { previousAnswer =>
        val preparedForm = previousAnswer match {
          case None => form(role)
          case Some(jsValue) =>
            (jsValue \ "whatDoYouWantToDo").validate[WhatDoYouWantToDo] match {
              case JsSuccess(chosenAction, _) => form(role).fill(chosenAction)
              case JsError(_) => form(role)
            }
        }
        Ok(view(preparedForm, role))
      }
  }

  def onSubmit(role: String): Action[AnyContent] = auth().async {
    implicit request =>
      form(role).bindFromRequest().fold(
        (formWithErrors: Form[_]) =>
          Future.successful(BadRequest(view(formWithErrors, role))),
        value => {
          val originalUserAnswers = managePensionsCacheConnector.fetch(request.externalId)

          managePensionsCacheConnector.save(request.externalId, WhatDoYouWantToDoId, value).map { cacheMap =>
            originalUserAnswers.map {
              case None =>
                Redirect(navigator.nextPage(WhatDoYouWantToDoId, NormalMode, UserAnswers()))
              case Some(jsValue) =>
                val chosenService = (cacheMap \ "whatDoYouWantToDo").as[WhatDoYouWantToDo]
                val updatedUserAnswers = jsValue.as[JsObject] + ("whatDoYouWantToDo" -> JsString(chosenService.toString))
                Redirect(navigator.nextPage(WhatDoYouWantToDoId, NormalMode, UserAnswers(updatedUserAnswers)))
            }
          }.flatten
        }
      )
  }
}
