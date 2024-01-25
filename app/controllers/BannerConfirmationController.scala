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

package controllers

import config.FrontendAppConfig
import controllers.actions.AuthAction
import models.AuthEntity.{PSA, PSP}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.banner_confirmation

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class BannerConfirmationController @Inject()(
                                 val appConfig: FrontendAppConfig,
                                 override val messagesApi: MessagesApi,
                                 authenticate: AuthAction,
                                 val controllerComponents: MessagesControllerComponents,
                                 view: banner_confirmation
                               )(implicit val ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport {

  def onPageLoadPsa: Action[AnyContent] = authenticate(PSA) {
    implicit request =>
      Ok(view())
  }

  def onPageLoadPsp: Action[AnyContent] = authenticate(PSP) {
    implicit request =>
      Ok(view())
  }
}
