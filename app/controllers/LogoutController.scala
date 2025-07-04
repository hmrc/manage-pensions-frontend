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
import connectors.UserAnswersCacheConnector
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions, MissingBearerToken}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.annotations.SessionDataCache

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class LogoutController @Inject()(
                                  override val authConnector: AuthConnector,
                                  appConfig: FrontendAppConfig,
                                  val controllerComponents: MessagesControllerComponents,
                                  @SessionDataCache sessionDataCacheConnector: UserAnswersCacheConnector
                                )(implicit ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport
    with AuthorisedFunctions {

  def onPageLoad: Action[AnyContent] = Action.async {
    implicit request =>
      authorised().retrieve(Retrievals.externalId) {
        case Some(id) =>
          sessionDataCacheConnector.removeAll(id).map { _ =>
            Redirect(appConfig.serviceSignOut)
          }
        case _ =>
          Future.successful(Redirect(routes.UnauthorisedController.onPageLoad))
      } recover {
        case _: MissingBearerToken =>
          Redirect(appConfig.serviceSignOut)
      }
  }

  def keepAlive: Action[AnyContent] = Action.async {
    Future successful Ok("OK")
  }
}
