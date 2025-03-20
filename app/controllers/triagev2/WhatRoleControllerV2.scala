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
import forms.triagev2.WhatRoleFormProviderV2
import identifiers.triagev2.WhatRoleId
import models.NormalMode
import models.triagev2.WhatRole
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{JsError, JsObject, JsString, JsSuccess}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.annotations.TriageV2
import utils.{Enumerable, Navigator, UserAnswers}
import views.html.triagev2.whatRole

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class WhatRoleControllerV2 @Inject()(override val messagesApi: MessagesApi,
                                     @TriageV2 navigator: Navigator,
                                     val auth: AuthAction,
                                     formProvider: WhatRoleFormProviderV2,
                                     val controllerComponents: MessagesControllerComponents,
                                     val managePensionsCacheConnector: ManagePensionsCacheConnector,
                                     val view: whatRole)
                                    (implicit val executionContext: ExecutionContext)
                                    extends FrontendBaseController with I18nSupport with Enumerable.Implicits with Retrievals {

  private def form: Form[WhatRole] = formProvider()

  def onPageLoad: Action[AnyContent] = auth().async {
    implicit request =>

      managePensionsCacheConnector.fetch(request.externalId).map { previousAnswer =>
        val preparedForm = previousAnswer match {
          case None => form
          case Some(jsValue) =>
            (jsValue \ "whatRole").validate[WhatRole] match {
              case JsSuccess(role, _) => form.fill(role)
              case JsError(_) => form
            }
        }
        Ok(view(preparedForm))
      }
  }

  def onSubmit: Action[AnyContent] = auth().async {
    implicit request =>
      form.bindFromRequest().fold(
        (formWithErrors: Form[_]) =>
          Future.successful(BadRequest(view(formWithErrors))),
        value => {
          val originalUserAnswers = managePensionsCacheConnector.fetch(request.externalId)

          managePensionsCacheConnector.save(request.externalId, WhatRoleId, value).map { cacheMap =>
            originalUserAnswers.map {
              case None =>
                val updatedUserAnswers = UserAnswers().set(WhatRoleId)(value).asOpt.getOrElse(UserAnswers())
                Redirect(navigator.nextPage(WhatRoleId, NormalMode, updatedUserAnswers))
              case Some(jsValue) =>
                val role = (cacheMap \ "whatRole").as[WhatRole]
                val updatedUserAnswers = jsValue.as[JsObject] + ("whatRole" -> JsString(role.toString))
                Redirect(navigator.nextPage(WhatRoleId, NormalMode, UserAnswers(updatedUserAnswers)))
            }
          }.flatten
        }
      )
  }
}
