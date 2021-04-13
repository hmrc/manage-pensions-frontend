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
import controllers.routes._
import identifiers.Identifier
import identifiers.psa.remove.{ConfirmRemovePsaId, PsaRemovalDateId}
import org.scalatest.prop.TableFor4
import play.api.libs.json.Json
import play.api.mvc.Call
import utils.NavigatorBehaviour
import utils.UserAnswerOps
import utils.UserAnswers

class RemovePSANavigatorSpec extends SpecBase with NavigatorBehaviour {

  import RemovePSANavigatorSpec._

  val navigator = new RemovePSANavigator(FakeUserAnswersCacheConnector)

  def routes(): TableFor4[Identifier, UserAnswers, Call, Option[Call]] = Table(
    ("Id",                 "User Answers",   "Next Page (NormalMode)",   "Next Page (CheckMode)"),
    (ConfirmRemovePsaId,   removePsa,           removalDatePage,           None),
    (ConfirmRemovePsaId,   dontRemovePsa,       schemeDetailsPage,         None),
    (ConfirmRemovePsaId,   emptyAnswers,        sessionExpiredPage,        None),
    (PsaRemovalDateId,     emptyAnswers,        confirmRemovedPage,        None)
  )

  navigator.getClass.getSimpleName must {
    appRunning()
    behave like nonMatchingNavigator(navigator)
    behave like navigatorWithRoutes(navigator, routes(), dataDescriber)
  }
}

object RemovePSANavigatorSpec {
  private val srn = "test srn"
  private lazy val emptyAnswers = UserAnswers(Json.obj())
  private lazy val removePsa = UserAnswers().srn(srn).confirmRemovePsa(isChecked = true)
  private lazy val dontRemovePsa = UserAnswers().srn(srn).confirmRemovePsa(isChecked = false)

  private def dataDescriber(answers: UserAnswers): String = answers.toString

  private val sessionExpiredPage = SessionExpiredController.onPageLoad()
  private val schemeDetailsPage = PsaSchemeDashboardController.onPageLoad(srn)
  private val removalDatePage = PsaRemovalDateController.onPageLoad()
  private val confirmRemovedPage = ConfirmRemovedController.onPageLoad()
}


