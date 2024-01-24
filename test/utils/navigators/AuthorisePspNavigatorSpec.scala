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
import controllers.invitations.psp.routes._
import identifiers.Identifier
import identifiers.invitations.psp._
import models.{CheckMode, Mode, NormalMode}
import models.requests.IdentifiedRequest
import org.scalatest.OptionValues
import org.scalatest.prop.TableFor4
import play.api.libs.json.Json
import play.api.mvc.Call
import uk.gov.hmrc.http.HeaderCarrier
import utils.{NavigatorBehaviour, UserAnswers}

class AuthorisePspNavigatorSpec extends SpecBase with NavigatorBehaviour {

  import AuthorisePspNavigatorSpec._

  val navigator = new AuthorisePspNavigator

  def routes(): TableFor4[Identifier, UserAnswers, Call, Option[Call]] = Table(
    ("Id",                            "User Answers",         "Next Page (NormalMode)",                   "Next Page (CheckMode)"),
    (PspNameId,                     emptyAnswers,                   pspIdPage,                                 Some(checkYourAnswer)),
    (PspId,                         emptyAnswers,                   pspHasClientRefPage(NormalMode),           Some(checkYourAnswer)),
    (PspHasClientReferenceId,       pspHasClientRefUserAns,         pspClientRefPage(NormalMode),              Some(pspClientRefPage(CheckMode))),
    (PspHasClientReferenceId,       pspHasClientRefUserAnsNo,       checkYourAnswer,                           Some(checkYourAnswer)),
    (PspClientReferenceId,          emptyAnswers,                   checkYourAnswer,                           Some(checkYourAnswer))
  )

  navigator.getClass.getSimpleName must {
    appRunning()
    behave like nonMatchingNavigator(navigator)
    behave like navigatorWithRoutes(navigator, routes(), dataDescriber)
  }
}

object AuthorisePspNavigatorSpec extends OptionValues {
  lazy val emptyAnswers: UserAnswers = UserAnswers(Json.obj())
  private lazy val pspHasClientRefUserAns: UserAnswers = UserAnswers().srn("srn").set(PspHasClientReferenceId)(true).asOpt.get
  private lazy val pspHasClientRefUserAnsNo: UserAnswers = UserAnswers().srn("srn").set(PspHasClientReferenceId)(false).asOpt.get
  lazy val checkYourAnswer: Call = CheckYourAnswersController.onPageLoad()
  lazy val pspIdPage: Call = PspIdController.onPageLoad(NormalMode)
   def pspClientRefPage(mode:Mode): Call = PspClientReferenceController.onPageLoad(mode)
   def pspHasClientRefPage(mode:Mode): Call = PspHasClientReferenceController.onPageLoad(mode)

  implicit val ex: IdentifiedRequest = new IdentifiedRequest() {
    val externalId: String = "test-external-id"
  }
  implicit val hc: HeaderCarrier = HeaderCarrier()

  private def dataDescriber(answers: UserAnswers): String = answers.toString

}
