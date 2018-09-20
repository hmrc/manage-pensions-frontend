/*
 * Copyright 2018 HM Revenue & Customs
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
import identifiers.{PsaNameId, Identifier}
import models.requests.IdentifiedRequest
import org.scalatest.OptionValues
import org.scalatest.prop.TableFor6
import play.api.libs.json.Json
import play.api.mvc.Call
import uk.gov.hmrc.http.HeaderCarrier
import utils.{NavigatorBehaviour, UserAnswers}

class InvitationNavigatorSpec extends SpecBase with NavigatorBehaviour {

  import InvitationNavigatorSpec._

  val navigator = new InvitationNavigator(FakeUserAnswersCacheConnector)

  def routes(): TableFor6[Identifier, UserAnswers, Call, Boolean, Option[Call], Boolean] = Table(
    ("Id", "User Answers", "Next Page (NormalMode)", "Save(NormalMode)", "Next Page (CheckMode)", "Save(CheckMode"),
    (PsaNameId, emptyAnswers, psaNamePage, true, None, false)
  )

  navigator.getClass.getSimpleName must {
    appRunning()
    behave like nonMatchingNavigator(navigator)
    behave like navigatorWithRoutes(navigator, FakeUserAnswersCacheConnector, routes(), dataDescriber)
  }
}

object InvitationNavigatorSpec extends OptionValues {
  lazy val emptyAnswers = UserAnswers(Json.obj())
  lazy val psaNamePage: Call = controllers.routes.IndexController.onPageLoad()


  implicit val ex: IdentifiedRequest = new IdentifiedRequest() {
    val externalId: String = "test-external-id"
  }
  implicit val hc: HeaderCarrier = HeaderCarrier()

  private def dataDescriber(answers: UserAnswers): String = answers.toString

}
