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
import identifiers.invitations._
import identifiers.{Identifier, SchemeSrnId}
import models.NormalMode
import org.scalatest.OptionValues
import org.scalatest.prop.TableFor4
import play.api.libs.json.Json
import play.api.mvc.Call
import utils.{NavigatorBehaviour, UserAnswers}

class AcceptInvitationNavigatorSpec extends SpecBase with NavigatorBehaviour {

  import AcceptInvitationNavigatorSpec._

  val navigator = new AcceptInvitationNavigator

  def routes(): TableFor4[Identifier, UserAnswers, Call, Option[Call]] = Table(
    ("Id", "User Answers", "Next Page (NormalMode)", "Next Page (CheckMode)"),
    (SchemeSrnId, emptyAnswers, doYouHaveWorkingKnowledge, None),
    (DoYouHaveWorkingKnowledgeId, employedAdviser, adviserDetails, None),
    (DoYouHaveWorkingKnowledgeId, noEmployedAdviser, declaration, None),
    (AdviserNameId, emptyAnswers, adviserEmail, None),
    (AdviserEmailId, emptyAnswers, adviserPostCodeLookup, None),
    (AdviserAddressPostCodeLookupId, emptyAnswers, adviserAddressList, None),
    (AdviserAddressListId, emptyAnswers, adviserManualAddress, None),
    (AdviserAddressId, emptyAnswers, checkYourAnswers, None),
    (CheckPensionAdviserAnswersId, emptyAnswers, declaration, None),
    (DeclarationId, emptyAnswers, inviteAccepted, None)
  )

  navigator.getClass.getSimpleName must {
    appRunning()
    behave like nonMatchingNavigator(navigator)
    behave like navigatorWithRoutes(navigator, routes(), dataDescriber)
  }
}

object AcceptInvitationNavigatorSpec extends OptionValues {
  private lazy val emptyAnswers = UserAnswers(Json.obj())
  private lazy val employedAdviser = UserAnswers().haveWorkingKnowledge(workingKnowledge = false)
  private lazy val noEmployedAdviser = UserAnswers().haveWorkingKnowledge(workingKnowledge = true)
  private lazy val doYouHaveWorkingKnowledge = controllers.invitations.routes.DoYouHaveWorkingKnowledgeController.onPageLoad(NormalMode)
  private lazy val adviserDetails = controllers.invitations.routes.AdviserDetailsController.onPageLoad(NormalMode)
  private lazy val declaration = controllers.invitations.routes.DeclarationController.onPageLoad()
  private lazy val adviserEmail = controllers.invitations.routes.AdviserEmailAddressController.onPageLoad(NormalMode)
  private lazy val adviserPostCodeLookup = controllers.invitations.routes.AdviserAddressPostcodeLookupController.onPageLoad()
  private lazy val adviserAddressList = controllers.invitations.routes.PensionAdviserAddressListController.onPageLoad(NormalMode)
  private lazy val adviserManualAddress = controllers.invitations.routes.AdviserManualAddressController.onPageLoad(NormalMode, prepopulated = true)
  private lazy val checkYourAnswers = controllers.invitations.routes.CheckPensionAdviserAnswersController.onPageLoad()
  private lazy val inviteAccepted = controllers.invitations.routes.InvitationAcceptedController.onPageLoad()
  private lazy val index: Call = controllers.routes.IndexController.onPageLoad()

  private def dataDescriber(answers: UserAnswers): String = answers.toString
}


