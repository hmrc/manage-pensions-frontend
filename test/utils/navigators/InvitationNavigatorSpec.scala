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
import identifiers.Identifier
import identifiers.invitations.{CheckYourAnswersId, InvitationSuccessId, InviteeNameId, InviteePSAId}
import models.NormalMode
import models.requests.IdentifiedRequest
import org.scalatest.OptionValues
import org.scalatest.prop.TableFor4
import play.api.libs.json.Json
import play.api.mvc.Call
import uk.gov.hmrc.http.HeaderCarrier
import utils.{NavigatorBehaviour, UserAnswers}

class InvitationNavigatorSpec extends SpecBase with NavigatorBehaviour {

  import InvitationNavigatorSpec._

  val navigator = new InvitationNavigator

  def routes(): TableFor4[Identifier, UserAnswers, Call, Option[Call]] = Table(
    ("Id",                            "User Answers",     "Next Page (NormalMode)",     "Next Page (CheckMode)"),
    (InviteeNameId,                     emptyAnswers,       psaIdPage,                    Some(checkYourAnswer)),
    (InviteePSAId,                      emptyAnswers,       checkYourAnswer,              Some(checkYourAnswer)),
    (CheckYourAnswersId(testSrn),       emptyAnswers,       invitationSuccess,            None),
    (InvitationSuccessId(testSrn),      emptyAnswers,       schemeDetails,                None)
  )

  navigator.getClass.getSimpleName must {
    appRunning()
    behave like nonMatchingNavigator(navigator)
    behave like navigatorWithRoutes(navigator, routes(), dataDescriber)
  }
}

object InvitationNavigatorSpec extends OptionValues {
  private val testSrn = "test-srn"
  lazy val emptyAnswers = UserAnswers(Json.obj())
  lazy val checkYourAnswer: Call = controllers.invitations.routes.CheckYourAnswersController.onPageLoad()
  lazy val invitationSuccess: Call = controllers.invitations.routes.InvitationSuccessController.onPageLoad(testSrn)
  lazy val schemeDetails: Call = controllers.routes.SchemeDetailsController.onPageLoad(testSrn)
  lazy val psaIdPage: Call = controllers.invitations.routes.PsaIdController.onPageLoad(NormalMode)

  implicit val ex: IdentifiedRequest = new IdentifiedRequest() {
    val externalId: String = "test-external-id"
  }
  implicit val hc: HeaderCarrier = HeaderCarrier()

  private def dataDescriber(answers: UserAnswers): String = answers.toString

}
