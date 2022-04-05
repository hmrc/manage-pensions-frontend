/*
 * Copyright 2022 HM Revenue & Customs
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

import config.FrontendAppConfig
import controllers.Retrievals
import controllers.actions.TriageAction
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.Enumerable
import views.html.triagev2.manageExistingScheme

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ManageExistingSchemeController @Inject()(override val messagesApi: MessagesApi,
                                               val appConfig: FrontendAppConfig,
                                               triageAction: TriageAction,
                                               val controllerComponents: MessagesControllerComponents,
                                               val view: manageExistingScheme
                                              )(implicit val executionContext: ExecutionContext
                                              ) extends FrontendBaseController with I18nSupport with Enumerable.Implicits with Retrievals {


  def onPageLoad(role: String): Action[AnyContent] = triageAction.async {
    implicit request =>
      val (managePensionSchemeLink, managePensionMigrationSchemeLink) = role match {
        case "administrator" => (s"${appConfig.loginUrl}?continue=${appConfig.loginToListSchemesUrl}",
          s"${appConfig.loginUrl}?continue=${appConfig.migrationListOfSchemesUrl}")
        case _ => (s"${appConfig.loginUrl}?continue=${appConfig.loginToListSchemesPspUrl}", "#")
      }
      val pensionSchemesOnlineLink = appConfig.tpssWelcomeUrl
      Future.successful(Ok(view(role,managePensionSchemeLink, managePensionMigrationSchemeLink, pensionSchemesOnlineLink)))
  }
}
