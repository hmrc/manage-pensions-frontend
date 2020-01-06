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

package utils.navigators

import base.SpecBase
import identifiers.Identifier
import identifiers.deregister.ConfirmStopBeingPsaId
import org.scalatest.OptionValues
import org.scalatest.prop.TableFor4
import play.api.libs.json.Json
import play.api.mvc.Call
import utils.{NavigatorBehaviour, UserAnswers}

class PsaDeRegistrationNavigatorSpec extends SpecBase with NavigatorBehaviour {

  import PsaDeRegistrationNavigatorSpec._

  private val navigator = new PsaDeRegistrationNavigator(frontendAppConfig)
  private lazy val psaDetailsPage: Call = Call("GET", frontendAppConfig.registeredPsaDetailsUrl)

  def routes(): TableFor4[Identifier, UserAnswers, Call, Option[Call]] = Table(
    ("Id", "User Answers", "Next Page (NormalMode)", "Next Page (CheckMode)"),
    (ConfirmStopBeingPsaId, confirmYes, successfulDeregistrationPage, None),
    (ConfirmStopBeingPsaId, confirmNo, psaDetailsPage, None)
  )

  navigator.getClass.getSimpleName must {
    appRunning()
    behave like nonMatchingNavigator(navigator)
    behave like navigatorWithRoutes(navigator, routes(), dataDescriber)
  }
}

object PsaDeRegistrationNavigatorSpec extends OptionValues {

  lazy val emptyAnswers: UserAnswers = UserAnswers(Json.obj())
  lazy val confirmYes: UserAnswers = UserAnswers(Json.obj()).set(ConfirmStopBeingPsaId)(value = true).asOpt.value
  lazy val confirmNo: UserAnswers = UserAnswers(Json.obj()).set(ConfirmStopBeingPsaId)(value = false).asOpt.value
  lazy val successfulDeregistrationPage: Call = controllers.deregister.routes.SuccessfulDeregistrationController.onPageLoad()

  private def dataDescriber(answers: UserAnswers): String = answers.toString

}
