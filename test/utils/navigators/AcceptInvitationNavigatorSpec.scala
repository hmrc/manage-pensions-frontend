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
import identifiers.{Identifier, SchemeSrnId}
import identifiers.invitations._
import models.NormalMode
import org.scalatest.OptionValues
import org.scalatest.prop.TableFor6
import play.api.libs.json.Json
import play.api.mvc.Call
import utils.{NavigatorBehaviour, UserAnswers}
import utils.UserAnswerOps

class AcceptInvitationNavigatorSpec extends SpecBase with NavigatorBehaviour {

  import AcceptInvitationNavigatorSpec._

  val navigator = new AcceptInvitationNavigator(FakeUserAnswersCacheConnector)

  def routes(): TableFor6[Identifier, UserAnswers, Call, Boolean, Option[Call], Boolean] = Table(
    ("Id",                             "User Answers",     "Next Page (NormalMode)",     "Save(NormalMode)",   "Next Page (CheckMode)", "Save(CheckMode"),
    (SchemeSrnId,                        emptyAnswers,       doYouHaveWorkingKnowledge,        false,                   None,                   false       ),
    (DoYouHaveWorkingKnowledgeId,        employedAdviser,    adviserDetails,                false,                   None,                   false       ),
    (DoYouHaveWorkingKnowledgeId,        noEmployedAdviser,  declaration,                   false,                   None,                   false       ),
    (AdviserNameId,                      emptyAnswers,       adviserEmail,                  false,                   None,                   false       ),
    (AdviserEmailId,                     emptyAnswers,       adviserPostCodeLookup,         false,                   None,                   false       ),
    (AdviserAddressPostCodeLookupId,     emptyAnswers,       adviserAddressList,            false,                   None,                   false       ),
    (AdviserAddressListId,               emptyAnswers,       adviserManualAddress,          false,                   None,                   false       ),
    (AdviserAddressId,                   emptyAnswers,       checkYourAnswers,              false,                   None,                   false       ),
    (CheckPensionAdviserAnswersId,       emptyAnswers,       declaration,                   false,                   None,                   false       ),
    (DeclarationId,                      emptyAnswers,       inviteAccepted,                false,                   None,                   false       )
  )

  navigator.getClass.getSimpleName must {
    appRunning()
    behave like nonMatchingNavigator(navigator)
    behave like navigatorWithRoutes(navigator, FakeUserAnswersCacheConnector, routes(), dataDescriber)
  }
}

object AcceptInvitationNavigatorSpec extends OptionValues {
  lazy val emptyAnswers = UserAnswers(Json.obj())
  lazy val employedAdviser = UserAnswers().haveWorkingKnowledge(false)
  lazy val noEmployedAdviser = UserAnswers().haveWorkingKnowledge(true)
  lazy val doYouHaveWorkingKnowledge = controllers.invitations.routes.DoYouHaveWorkingKnowledgeController.onPageLoad(NormalMode)
  lazy val adviserDetails = controllers.invitations.routes.AdviserDetailsController.onPageLoad(NormalMode)
  lazy val declaration = controllers.invitations.routes.DeclarationController.onPageLoad()
  lazy val adviserEmail = controllers.invitations.routes.AdviserEmailAddressController.onPageLoad(NormalMode)
  lazy val adviserPostCodeLookup = controllers.invitations.routes.AdviserAddressPostcodeLookupController.onPageLoad()
  lazy val adviserAddressList = controllers.invitations.routes.PensionAdviserAddressListController.onPageLoad(NormalMode)
  lazy val adviserManualAddress = controllers.invitations.routes.AdviserManualAddressController.onPageLoad(NormalMode, true)
  lazy val checkYourAnswers = controllers.invitations.routes.CheckPensionAdviserAnswersController.onPageLoad()
  lazy val inviteAccepted = controllers.invitations.routes.InvitationAcceptedController.onPageLoad()
  lazy val index: Call = controllers.routes.IndexController.onPageLoad()

  private def dataDescriber(answers: UserAnswers): String = answers.toString
}


