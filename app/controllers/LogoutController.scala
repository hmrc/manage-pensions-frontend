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

package controllers

import config.FrontendAppConfig
import connectors.UserAnswersCacheConnector
import connectors.aft.AftCacheConnector

import javax.inject.Inject
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.{AuthorisedFunctions, AuthConnector}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.annotations.SessionDataCache

import scala.concurrent.{ExecutionContext, Future}

class LogoutController @Inject()(
                                  override val authConnector: AuthConnector,
                                  appConfig: FrontendAppConfig,
                                  aftCacheConnector: AftCacheConnector,
                                  val controllerComponents: MessagesControllerComponents,
                                  @SessionDataCache userAnswersCacheConnector: UserAnswersCacheConnector
                                )(implicit ec : ExecutionContext) extends FrontendBaseController with I18nSupport with AuthorisedFunctions {

  def onPageLoad: Action[AnyContent] = Action.async {
    implicit request =>
      authorised().retrieve(Retrievals.externalId) {
        case Some(id) =>
          userAnswersCacheConnector.removeAll(id).flatMap{ _ =>
            aftCacheConnector.removeLock.map {_ =>
              Redirect(appConfig.serviceSignOut).withNewSession
            }
          }
        case _ =>
          Future.successful(Redirect(routes.UnauthorisedController.onPageLoad()))
      }
  }
}
