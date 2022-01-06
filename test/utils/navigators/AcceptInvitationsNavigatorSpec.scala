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
import controllers.invitations.psa.routes._
import controllers.invitations.routes._
import identifiers.{Identifier, SchemeSrnId}
import identifiers.invitations._
import identifiers.invitations.psa.{AdviserAddressId, AdviserAddressListId, AdviserAddressPostCodeLookupId, AdviserEmailId, AdviserNameId, CheckPensionAdviserAnswersId}
import models.NormalMode
import org.scalatest.OptionValues
import org.scalatest.prop.TableFor4
import play.api.libs.json.Json
import play.api.mvc.Call
import utils.{NavigatorBehaviour, UserAnswers}

class AcceptInvitationsNavigatorSpec
  extends SpecBase
    with NavigatorBehaviour {

  import AcceptInvitationsNavigatorSpec._

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

object AcceptInvitationsNavigatorSpec extends OptionValues {
  private lazy val emptyAnswers = UserAnswers(Json.obj())
  private lazy val employedAdviser = UserAnswers().haveWorkingKnowledge(workingKnowledge = false)
  private lazy val noEmployedAdviser = UserAnswers().haveWorkingKnowledge(workingKnowledge = true)
  private lazy val doYouHaveWorkingKnowledge = DoYouHaveWorkingKnowledgeController.onPageLoad(NormalMode)
  private lazy val adviserDetails = AdviserDetailsController.onPageLoad(NormalMode)
  private lazy val declaration = DeclarationController.onPageLoad()
  private lazy val adviserEmail = AdviserEmailAddressController.onPageLoad(NormalMode)
  private lazy val adviserPostCodeLookup = AdviserAddressPostcodeLookupController.onPageLoad()
  private lazy val adviserAddressList = PensionAdviserAddressListController.onPageLoad(NormalMode)
  private lazy val adviserManualAddress = AdviserManualAddressController.onPageLoad(NormalMode, prepopulated = true)
  private lazy val checkYourAnswers = CheckPensionAdviserAnswersController.onPageLoad()
  private lazy val inviteAccepted = InvitationAcceptedController.onPageLoad()

  private def dataDescriber(answers: UserAnswers): String = answers.toString
}


