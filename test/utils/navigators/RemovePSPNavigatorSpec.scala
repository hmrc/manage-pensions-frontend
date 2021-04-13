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

package utils.navigators

import base.SpecBase
import connectors.FakeUserAnswersCacheConnector
import controllers.psa.routes._
import controllers.psa.remove.routes._
import controllers.psp.deauthorise.routes._
import controllers.routes._
import identifiers.Identifier
import identifiers.remove.psa.PsaRemovePspDeclarationId
import identifiers.remove.psp
import identifiers.remove.psp.PspRemovalDateId
import org.scalatest.prop.TableFor4
import play.api.libs.json.Json
import play.api.mvc.Call
import utils.{NavigatorBehaviour, UserAnswers}

class RemovePSPNavigatorSpec extends SpecBase with NavigatorBehaviour {

  import RemovePSPNavigatorSpec._

  val navigator = new RemovePSPNavigator(FakeUserAnswersCacheConnector)

  def routes(): TableFor4[Identifier, UserAnswers, Call, Option[Call]] = Table(
    ("Id",                        "User Answers",   "Next Page (NormalMode)",   "Next Page (CheckMode)"),
    (psp.ConfirmRemovePspId(0),         removePsp,           pspRemovalDatePage,           None),
    (psp.ConfirmRemovePspId(0),         dontRemovePsp,       schemeDetailsPage,            None),
    (psp.ConfirmRemovePspId(0),         emptyAnswers,        sessionExpiredPage,           None),
    (PspRemovalDateId(0),           emptyAnswers,        psaRemovePspDeclarationPage,  None),
    (PsaRemovePspDeclarationId(0),  emptyAnswers,        psaRemovePspConfirmationPage, None)
  )

  navigator.getClass.getSimpleName must {
    appRunning()
    behave like nonMatchingNavigator(navigator)
    behave like navigatorWithRoutes(navigator, routes(), dataDescriber)
  }
}

object RemovePSPNavigatorSpec {
  private val srn = "test srn"
  private lazy val emptyAnswers: UserAnswers = UserAnswers(Json.obj())
  private lazy val removePsp: UserAnswers = UserAnswers().srn(srn).set(psp.ConfirmRemovePspId(0))(true).asOpt.get
  private lazy val dontRemovePsp: UserAnswers = UserAnswers().srn(srn).set(psp.ConfirmRemovePspId(0))(false).asOpt.get

  private def dataDescriber(answers: UserAnswers): String = answers.toString

  private val sessionExpiredPage = SessionExpiredController.onPageLoad()
  private val schemeDetailsPage = PsaSchemeDashboardController.onPageLoad(srn)
  private val pspRemovalDatePage = PspRemovalDateController.onPageLoad(0)
  private val psaRemovePspDeclarationPage = PsaRemovePspDeclarationController.onPageLoad(0)
  private val psaRemovePspConfirmationPage = ConfirmPsaRemovedPspController.onPageLoad(0)
}




