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
import controllers.actions.{AuthAction, DataRetrievalAction, TriageAction}
import forms.triagev2.WhichServiceYouWantToViewFormProvider
import identifiers.triagev2.{WhatRoleId, WhichServiceYouWantToViewId}
import models.NormalMode
import models.triagev2.{WhatRole, WhichServiceYouWantToView}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.annotations.TriageV2
import utils.{Enumerable, Navigator, UserAnswers}
import views.html.triagev2.whichServiceYouWantToView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json._

class WhichServiceYouWantToViewController @Inject()(override val messagesApi: MessagesApi,
                                                    @TriageV2 navigator: Navigator,
                                                    auth: AuthAction,
                                                    formProvider: WhichServiceYouWantToViewFormProvider,
                                                    val controllerComponents: MessagesControllerComponents,
                                                    val view: whichServiceYouWantToView,
                                                    val managePensionsCacheConnector: ManagePensionsCacheConnector
                                           )(implicit val executionContext: ExecutionContext
                                           ) extends FrontendBaseController with I18nSupport with Enumerable.Implicits with Retrievals {

  private def form(role: String): Form[WhichServiceYouWantToView] = formProvider(role)

  def onPageLoad(role: String): Action[AnyContent] = auth().async {
    implicit request =>
      managePensionsCacheConnector.fetch(request.externalId).map { previousAnswer =>
        val preparedForm = previousAnswer match {
          case None => form(role)
          case Some(jsValue) =>
            (jsValue \ "whichServiceYouWantToView").validate[WhichServiceYouWantToView] match {
              case JsSuccess(chosenService, _) => form(role).fill(chosenService)
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

          managePensionsCacheConnector.save(request.externalId, WhichServiceYouWantToViewId, value).map { cacheMap =>
            originalUserAnswers.map {
              case None =>
                Redirect(navigator.nextPage(WhichServiceYouWantToViewId, NormalMode, UserAnswers()))
              case Some(jsValue) =>
                val chosenService = (cacheMap \ "whichServiceYouWantToView").as[WhichServiceYouWantToView]
                val updatedUserAnswers = jsValue.as[JsObject] + ("whichServiceYouWantToView" -> JsString(chosenService.toString))
                Redirect(navigator.nextPage(WhichServiceYouWantToViewId, NormalMode, UserAnswers(updatedUserAnswers)))
            }
          }.flatten
        }
      )
  }
}
