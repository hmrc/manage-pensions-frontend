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

package utils.navigators

import base.SpecBase
import connectors.FakeUserAnswersCacheConnector
import controllers.psa.routes._
import controllers.psp.deauthorise.routes._
import controllers.routes._
import identifiers.Identifier
import identifiers.psp.deauthorise
import identifiers.psp.deauthorise.ConfirmDeauthorisePspId
import org.scalatest.prop.TableFor4
import play.api.libs.json.Json
import play.api.mvc.Call
import utils.{NavigatorBehaviour, UserAnswers}

class DeauthorisePSPNavigatorSpec extends SpecBase with NavigatorBehaviour {

  import DeauthorisePSPNavigatorSpec._

  val navigator = new DeauthorisePSPNavigator(FakeUserAnswersCacheConnector)

  def routes(): TableFor4[Identifier, UserAnswers, Call, Option[Call]] = Table(
    ("Id", "User Answers", "Next Page (NormalMode)", "Next Page (CheckMode)"),
    (ConfirmDeauthorisePspId(0), deauthPsp, pspDeauthDatePage, None),
    (deauthorise.ConfirmDeauthorisePspId(0), dontDeauthPsp, schemeDetailsPage, None),
    (deauthorise.ConfirmDeauthorisePspId(0), emptyAnswers, sessionExpiredPage, None),
    (deauthorise.PspDeauthDateId(0), emptyAnswers, psaDeauthPspDeclarationPage, None),
    (deauthorise.PsaDeauthorisePspDeclarationId(0), emptyAnswers, psaDeauthPspConfirmationPage, None)
  )

  navigator.getClass.getSimpleName must {
    appRunning()
    behave like nonMatchingNavigator(navigator)
    behave like navigatorWithRoutes(navigator, routes(), dataDescriber)
  }
}

object DeauthorisePSPNavigatorSpec {
  private val srn = "test srn"
  private lazy val emptyAnswers: UserAnswers = UserAnswers(Json.obj())
  private lazy val deauthPsp: UserAnswers = UserAnswers().srn(srn).set(deauthorise.ConfirmDeauthorisePspId(0))(true).asOpt.get
  private lazy val dontDeauthPsp: UserAnswers = UserAnswers().srn(srn).set(deauthorise.ConfirmDeauthorisePspId(0))(false).asOpt.get

  private def dataDescriber(answers: UserAnswers): String = answers.toString

  private val sessionExpiredPage = SessionExpiredController.onPageLoad
  private val schemeDetailsPage = PsaSchemeDashboardController.onPageLoad(srn)
  private val pspDeauthDatePage = PspDeauthDateController.onPageLoad(0)
  private val psaDeauthPspDeclarationPage = PsaDeauthPspDeclarationController.onPageLoad(0)
  private val psaDeauthPspConfirmationPage = ConfirmPsaDeauthPspController.onPageLoad(0)
}




