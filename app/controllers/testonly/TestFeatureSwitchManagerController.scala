/*
 * Copyright 2019 HM Revenue & Customs
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

package controllers.testonly

import com.google.inject.Inject
import config.FeatureSwitchManagementService
import connectors.{PensionAdministratorFeatureSwitchConnectorImpl, PensionsSchemeFeatureSwitchConnectorImpl}
import play.api.Logger
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.ExecutionContext

class TestFeatureSwitchManagerController @Inject()(
                                                    fs: FeatureSwitchManagementService,
                                                    schemeFeatureSwitchConnector: PensionsSchemeFeatureSwitchConnectorImpl,
                                                    adminFeatureSwitchConnector: PensionAdministratorFeatureSwitchConnectorImpl
                                                  )(implicit val ec: ExecutionContext) extends FrontendController {

  def toggleOn(featureSwitch: String): Action[AnyContent] = Action.async {
    implicit request =>
      val frontEndToggledOn = fs.change(featureSwitch, newValue = true)
      if (frontEndToggledOn) {
        Logger.debug(s"[Manage-Pensions-Frontend][ToggleOnSuccess] - ${featureSwitch}")
      } else {
        Logger.debug(s"[Manage-Pensions-Frontend][ToggleOnFailed] - ${featureSwitch}")
      }
      for {
        schemeToggledOn <- schemeFeatureSwitchConnector.toggleOn(featureSwitch)
        adminToggledOn <- adminFeatureSwitchConnector.toggleOn(featureSwitch)
      } yield {
        if (schemeToggledOn && adminToggledOn && frontEndToggledOn) {
          NoContent
        } else {
          ExpectationFailed
        }
      }
  }

  def toggleOff(featureSwitch: String): Action[AnyContent] = Action.async {
    implicit request =>
      val frontEndToggledOff = fs.change(featureSwitch, newValue = false)
      if (frontEndToggledOff) {
        Logger.debug(s"[Manage-Pensions-Frontend][ToggleOffSuccess] - ${featureSwitch}")
      } else {
        Logger.debug(s"[Manage-Pensions-Frontend][ToggleOffFailed] - ${featureSwitch}")
      }
      for {
        schemeToggledOff <- schemeFeatureSwitchConnector.toggleOff(featureSwitch)
        adminToggledOff <- adminFeatureSwitchConnector.toggleOff(featureSwitch)
      } yield {
        if (schemeToggledOff && adminToggledOff && frontEndToggledOff) {
          NoContent
        } else {
          ExpectationFailed
        }
      }
  }

  def reset(featureSwitch: String): Action[AnyContent] = Action.async {
    implicit request =>
      fs.reset(featureSwitch)
      Logger.debug(s"[Manage-Pensions-Frontend][ToggleResetSuccess] - ${featureSwitch}")
      for {
        schemeReset <- schemeFeatureSwitchConnector.reset(featureSwitch)
        adminReset <- adminFeatureSwitchConnector.reset(featureSwitch)
      } yield {
        if (schemeReset && adminReset) {
          NoContent
        } else {
          ExpectationFailed
        }
      }
  }
}
