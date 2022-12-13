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

package utils.navigators

import base.SpecBase
import connectors.FakeUserAnswersCacheConnector
import controllers.psp.deauthorise.self.routes._
import controllers.psp.routes._
import controllers.routes._
import identifiers.Identifier
import identifiers.psp.deauthorise.self.{ConfirmDeauthId, DeauthDateId}
import org.scalatest.prop.TableFor4
import play.api.libs.json.Json
import play.api.mvc.Call
import utils.{NavigatorBehaviour, UserAnswerOps, UserAnswers}
import utils.navigators.PspSelfDeauthNavigatorSpec._

class PspSelfDeauthNavigatorSpec extends SpecBase with NavigatorBehaviour {

  val navigator = new PspSelfDeauthoriseNavigator(FakeUserAnswersCacheConnector)

  def routes(): TableFor4[Identifier, UserAnswers, Call, Option[Call]] = Table(
    ("Id", "User Answers", "Next Page (NormalMode)", "Next Page (CheckMode)"),
    (ConfirmDeauthId, deauthPsp, deauthDatePage, None),
    (ConfirmDeauthId, dontDeauthPsp, schemeDashboardPage, None),
    (ConfirmDeauthId, emptyAnswers, sessionExpiredPage, None),
    (DeauthDateId, emptyAnswers, declarationPage, None)
  )

  navigator.getClass.getSimpleName must {
    appRunning()
    behave like nonMatchingNavigator(navigator)
    behave like navigatorWithRoutes(navigator, routes(), dataDescriber)
  }
}

object PspSelfDeauthNavigatorSpec {
  private val srn = "test srn"
  private lazy val emptyAnswers = UserAnswers(Json.obj())
  private lazy val deauthPsp = UserAnswers().srn(srn).set(ConfirmDeauthId)(true).asOpt.get
  private lazy val dontDeauthPsp = UserAnswers().srn(srn).set(ConfirmDeauthId)(false).asOpt.get

  private def dataDescriber(answers: UserAnswers): String = answers.toString

  private val sessionExpiredPage = SessionExpiredController.onPageLoad
  private val schemeDashboardPage = PspSchemeDashboardController.onPageLoad(srn)
  private val deauthDatePage = DeauthDateController.onPageLoad()
  private val declarationPage = DeclarationController.onPageLoad()
}


