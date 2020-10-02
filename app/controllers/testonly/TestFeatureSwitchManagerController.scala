/*
 * Copyright 2020 HM Revenue & Customs
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
import connectors.admin.PensionAdministratorFeatureSwitchConnectorImpl
import connectors.scheme.PensionsSchemeFeatureSwitchConnectorImpl
import play.api.Logger
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import play.api.mvc.Request
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import views.html.testOnly.testFeatureSwitchManagerSuccess

import scala.concurrent.ExecutionContext

class TestFeatureSwitchManagerController @Inject()(
                                                    fs: FeatureSwitchManagementService,
                                                    schemeFeatureSwitchConnector: PensionsSchemeFeatureSwitchConnectorImpl,
                                                    adminFeatureSwitchConnector: PensionAdministratorFeatureSwitchConnectorImpl,
                                                    val controllerComponents: MessagesControllerComponents
                                                  )(implicit val ec: ExecutionContext) extends FrontendBaseController {

  private def onOrOff(state:Option[Boolean]):String = state match {
    case None => "unknown"
    case Some(true) => "true"
    case _ => "false"
  }

  private def switches(featureSwitch:String,
                       frontendState: Option[Boolean],
                       schemeState:Option[Boolean],
                       adminState:Option[Boolean]):Map[String,String] = Map(
    "pensions-scheme-frontend" -> onOrOff(frontendState),
    "pensions-scheme" -> onOrOff(schemeState),
    "pension-administrator" -> onOrOff(adminState)
  )

  private def successMessage(featureSwitch:String, newToggleState:Boolean) =
    s"""Feature switch "$featureSwitch" successfully set to ${onOrOff(Option(newToggleState))} in all services listed below."""
  private def failureMessage(featureSwitch:String, newToggleState:Boolean) =
    s"""Unable to set feature switch "$featureSwitch" to ${onOrOff(Option(newToggleState))} in all services listed below."""

  private def getView(featureSwitch:String,
                      newToggleState:Boolean,
                      frontEndState:Option[Boolean],
                      schemeState:Option[Boolean],
                      adminState:Option[Boolean])(implicit request:Request[_]) = {
    if (frontEndState.contains(newToggleState) && schemeState.contains(newToggleState) && adminState.contains(newToggleState)) {
      Ok(testFeatureSwitchManagerSuccess("""Request to set feature switch successful.""",
        s"""Current values of feature switch "$featureSwitch":-""",
        successMessage(featureSwitch, newToggleState),
        switches(featureSwitch, frontEndState, schemeState, adminState)))
    } else {
      ExpectationFailed(testFeatureSwitchManagerSuccess("""Request to set feature switch unsuccessful.""",
        s"""Current values of feature switch "$featureSwitch":-""",
        failureMessage(featureSwitch, newToggleState),
        switches(featureSwitch, frontEndState, schemeState, adminState)))
    }
  }

  private def getResetView(featureSwitch:String,
                           frontEndState:Option[Boolean],
                           schemeState:Option[Boolean],
                           adminState:Option[Boolean])(implicit request:Request[_]) = {

    Ok(testFeatureSwitchManagerSuccess("""Request to reset feature switch successful.""",
      s"""Current values of feature switch "$featureSwitch":-""",
      s"""$featureSwitch has been reset""",
      switches(featureSwitch, frontEndState, schemeState, adminState))
    )
  }

  def toggleOn(featureSwitch: String): Action[AnyContent] = Action.async {
    implicit request =>
      val frontEndToggledOn = fs.change(featureSwitch, newValue = true)
      if (frontEndToggledOn) {
        Logger.debug(s"[Manage-Pensions-frontend][ToggleOnSuccess] - $featureSwitch")
      } else {
        Logger.debug(s"[Manage-Pensions-frontend][ToggleOnFailed] - $featureSwitch")
      }
      for {
        _ <- schemeFeatureSwitchConnector.toggleOn(featureSwitch)
        _ <- adminFeatureSwitchConnector.toggleOn(featureSwitch)
        schemeCurrentValue <- schemeFeatureSwitchConnector.get(featureSwitch)
        adminCurrentValue <- adminFeatureSwitchConnector.get(featureSwitch)
      } yield {
        getView(featureSwitch, newToggleState = true, Option(fs.get(featureSwitch)), schemeCurrentValue, adminCurrentValue)
      }
  }

  def toggleOff(featureSwitch: String): Action[AnyContent] = Action.async {
    implicit request =>
      val frontEndToggledOff = fs.change(featureSwitch, newValue = false)
      if (frontEndToggledOff) {
        Logger.debug(s"[Manage-Pensions-frontend][ToggleOffSuccess] - $featureSwitch")
      } else {
        Logger.debug(s"[Manage-Pensions-frontend][ToggleOffFailed] - $featureSwitch")
      }
      for {
        _ <- schemeFeatureSwitchConnector.toggleOff(featureSwitch)
        _ <- adminFeatureSwitchConnector.toggleOff(featureSwitch)
        schemeCurrentValue <- schemeFeatureSwitchConnector.get(featureSwitch)
        adminCurrentValue <- adminFeatureSwitchConnector.get(featureSwitch)
      } yield {
        getView(featureSwitch, newToggleState = false, Option(fs.get(featureSwitch)), schemeCurrentValue, adminCurrentValue)
      }
  }

  def reset(featureSwitch: String): Action[AnyContent] = Action.async {implicit request =>
    fs.reset(featureSwitch)
    for {
      _ <- schemeFeatureSwitchConnector.reset(featureSwitch)
      _ <- adminFeatureSwitchConnector.reset(featureSwitch)
      schemeCurrentValue <- schemeFeatureSwitchConnector.get(featureSwitch)
      adminCurrentValue <- adminFeatureSwitchConnector.get(featureSwitch)
    } yield {
      getResetView(featureSwitch, Option(fs.get(featureSwitch)), schemeCurrentValue, adminCurrentValue)
    }
  }
}
