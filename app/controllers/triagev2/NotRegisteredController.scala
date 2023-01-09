/*
 * Copyright 2023 HM Revenue & Customs
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

import com.google.inject.Inject
import controllers.actions.TriageAction
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.triagev2.notRegistered

import scala.concurrent.{ExecutionContext, Future}

class NotRegisteredController @Inject()(override val messagesApi: MessagesApi,
                                        triageAction: TriageAction,
                                        val controllerComponents: MessagesControllerComponents,
                                        view: notRegistered
                                        )(implicit val ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad: Action[AnyContent] = triageAction.async {
    implicit request =>

      Future.successful(Ok(view()))
  }
}
