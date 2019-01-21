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

package utils.navigators

import base.SpecBase
import connectors.FakeUserAnswersCacheConnector
import identifiers.Identifier
import identifiers.deregister.ConfirmStopBeingPsaId
import org.scalatest.OptionValues
import org.scalatest.prop.TableFor6
import play.api.libs.json.Json
import play.api.mvc.Call
import utils.{NavigatorBehaviour, UserAnswers}

class PsaDeRegistrationNavigatorSpec extends SpecBase with NavigatorBehaviour {

  import PsaDeRegistrationNavigatorSpec._

  val navigator = new PsaDeRegistrationNavigator(FakeUserAnswersCacheConnector, frontendAppConfig)
  lazy val psaDetailsPage: Call = new Call("GET", frontendAppConfig.registeredPsaDetailsUrl)

  def routes(): TableFor6[Identifier, UserAnswers, Call, Boolean, Option[Call], Boolean] = Table(
    ("Id", "User Answers", "Next Page (NormalMode)", "Save(NormalMode)", "Next Page (CheckMode)", "Save(CheckMode"),
    (ConfirmStopBeingPsaId, confirmYes, successfulDeregistrationPage, false, None, false),
    (ConfirmStopBeingPsaId, confirmNo, psaDetailsPage, false, None, false)
  )

  navigator.getClass.getSimpleName must {
    appRunning()
    behave like nonMatchingNavigator(navigator)
    behave like navigatorWithRoutes(navigator, FakeUserAnswersCacheConnector, routes(), dataDescriber)
  }
}

object PsaDeRegistrationNavigatorSpec extends OptionValues {

  lazy val emptyAnswers: UserAnswers = UserAnswers(Json.obj())
  lazy val confirmYes: UserAnswers = UserAnswers(Json.obj()).set(ConfirmStopBeingPsaId)(value = true).asOpt.value
  lazy val confirmNo: UserAnswers = UserAnswers(Json.obj()).set(ConfirmStopBeingPsaId)(value = false).asOpt.value
  lazy val successfulDeregistrationPage: Call = controllers.routes.SessionExpiredController.onPageLoad()

  private def dataDescriber(answers: UserAnswers): String = answers.toString

}
