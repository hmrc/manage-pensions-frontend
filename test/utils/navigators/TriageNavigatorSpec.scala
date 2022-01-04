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
import identifiers.Identifier
import identifiers.triage._
import models.triage.DoesPSAStartWithATwo.{No, Yes}
import models.triage.{DoesPSAStartWithATwo, DoesPSTRStartWithATwo, WhatDoYouWantToDo}
import models.triage.WhatDoYouWantToDo.{BecomeAnAdmin, ChangeAdminDetails, CheckTheSchemeStatus, Invite, ManageExistingScheme, UpdateSchemeInformation}
import models.triage.WhatRole.PSA
import org.scalatest.OptionValues
import org.scalatest.prop.TableFor4
import play.api.libs.json.Json
import play.api.mvc.Call
import utils.{Enumerable, NavigatorBehaviour, UserAnswers}

class TriageNavigatorSpec extends SpecBase with NavigatorBehaviour {

  import TriageNavigatorSpec._

  private val navigator = new TriageNavigator(frontendAppConfig)

  private def loginToYourSchemesPage: Call = Call("GET", s"${frontendAppConfig.loginUrl}?continue=${frontendAppConfig.loginToListSchemesUrl}")

  private def loginToChangePsaDetailsPage: Call = Call("GET", s"${frontendAppConfig.loginUrl}?continue=${frontendAppConfig.registeredPsaDetailsUrl}")

  private def tpssWelcomePage: Call = Call("GET", frontendAppConfig.tpssWelcomeUrl)

  private def pensionSchemesInvitationGuideGovUkPage: Call = Call("GET", frontendAppConfig.pensionSchemesInvitationGuideGovUkLink)

  private def pensionSchemesGuideGovUkPage: Call = Call("GET", frontendAppConfig.pensionSchemesGuideMandatoryOnlineFilingGovUkLink)

  def routes(): TableFor4[Identifier, UserAnswers, Call, Option[Call]] = Table(
    ("Id", "User Answers", "Next Page (NormalMode)", "Next Page (CheckMode)"),
    (WhatDoYouWantToDoId, whatDoYouWantToDoAnswers(ManageExistingScheme), doesPSTRTStartWithTwoPage, None),
    (WhatDoYouWantToDoId, whatDoYouWantToDoAnswers(CheckTheSchemeStatus), loginToYourSchemesPage, None),
    (WhatDoYouWantToDoId, whatDoYouWantToDoAnswers(Invite), doesPSTRTStartWithTwoInvitePage, None),
    (WhatDoYouWantToDoId, whatDoYouWantToDoAnswers(BecomeAnAdmin), doesPSTRTStartWithTwoInvitedPage, None),
    (WhatDoYouWantToDoId, whatDoYouWantToDoAnswers(UpdateSchemeInformation), doesPSTRTStartWithTwoUpdatePage, None),
    (WhatDoYouWantToDoId, whatDoYouWantToDoAnswers(ChangeAdminDetails), doesPSATStartWithATwoPage, None),
    (WhatDoYouWantToDoId, emptyAnswers, sessionExpiredPage, None),
    (DoesPSTRStartWithTwoId, doesPSTRStartWithTwoAnswers(DoesPSTRStartWithATwo.Yes), loginToYourSchemesPage, None),
    (DoesPSTRStartWithTwoId, doesPSTRStartWithTwoAnswers(DoesPSTRStartWithATwo.No), tpssWelcomePage, None),
    (DoesPSTRStartWithTwoId, emptyAnswers, sessionExpiredPage, None),
    (DoesPSTRStartWithTwoInviteId, doesPSTRStartWithTwoInviteAnswers(DoesPSTRStartWithATwo.Yes), invitingPSTRStartWithTwoPage, None),
    (DoesPSTRStartWithTwoInviteId, doesPSTRStartWithTwoInviteAnswers(DoesPSTRStartWithATwo.No), pensionSchemesInvitationGuideGovUkPage, None),
    (DoesPSTRStartWithTwoInviteId, emptyAnswers, sessionExpiredPage, None),
    (DoesPSTRStartWithTwoUpdateId, doesPSTRStartWithTwoUpdateAnswers(DoesPSTRStartWithATwo.Yes), updatingPSTRStartWithTwoPage, None),
    (DoesPSTRStartWithTwoUpdateId, doesPSTRStartWithTwoUpdateAnswers(DoesPSTRStartWithATwo.No), pensionSchemesGuideGovUkPage, None),
    (DoesPSTRStartWithTwoUpdateId, emptyAnswers, sessionExpiredPage, None),
    (DoesPSAStartWithATwoId, doesPSAStartWithATwoAnswers(Yes), loginToChangePsaDetailsPage, None),
    (DoesPSAStartWithATwoId, doesPSAStartWithATwoAnswers(No), updateBothPage, None),
    (DoesPSAStartWithATwoId, emptyAnswers, sessionExpiredPage, None)
  )

  navigator.getClass.getSimpleName must {
    appRunning()
    behave like nonMatchingNavigator(navigator)
    behave like navigatorWithRoutes(navigator, routes(), dataDescriber)
  }
}

object TriageNavigatorSpec extends OptionValues with Enumerable.Implicits {

  lazy val emptyAnswers: UserAnswers = UserAnswers(Json.obj())
  lazy val whatRolePsaAnswers: UserAnswers = UserAnswers().set(WhatRoleId)(PSA).asOpt.value

  private def whatDoYouWantToDoAnswers(answer: WhatDoYouWantToDo): UserAnswers =
    whatRolePsaAnswers.set(WhatDoYouWantToDoId)(answer)(writes(WhatDoYouWantToDo.enumerable("PSA"))).asOpt.value

  private def doesPSAStartWithATwoAnswers(answer: DoesPSAStartWithATwo): UserAnswers =
    whatRolePsaAnswers.set(DoesPSAStartWithATwoId)(answer).asOpt.value

  private def doesPSTRStartWithTwoAnswers(answer: DoesPSTRStartWithATwo): UserAnswers =
    whatRolePsaAnswers.set(DoesPSTRStartWithTwoId)(answer).asOpt.value

  private def doesPSTRStartWithTwoInviteAnswers(answer: DoesPSTRStartWithATwo): UserAnswers =
    whatRolePsaAnswers.set(DoesPSTRStartWithTwoInviteId)(answer).asOpt.value

  private def doesPSTRStartWithTwoUpdateAnswers(answer: DoesPSTRStartWithATwo): UserAnswers =
    UserAnswers().set(DoesPSTRStartWithTwoUpdateId)(answer).asOpt.value

  private def doesPSTRTStartWithTwoPage: Call = controllers.triage.routes.DoesPSTRStartWithTwoController.onPageLoad("PSA")

  private def doesPSTRTStartWithTwoInvitePage: Call = controllers.triage.routes.DoesPSTRStartWithTwoInviteController.onPageLoad()

  private def doesPSTRTStartWithTwoInvitedPage: Call = controllers.triage.routes.DoesPSTRStartWithTwoInvitedController.onPageLoad()

  private def doesPSTRTStartWithTwoUpdatePage: Call = controllers.triage.routes.DoesPSTRStartWithTwoUpdateController.onPageLoad()

  private def doesPSATStartWithATwoPage: Call = controllers.triage.routes.DoesPSAStartWithATwoController.onPageLoad()

  private def invitingPSTRStartWithTwoPage: Call = controllers.triage.routes.InvitingPSTRStartWithTwoController.onPageLoad()

  private def updatingPSTRStartWithTwoPage: Call = controllers.triage.routes.UpdatingPSTRStartWithTwoController.onPageLoad()

  private def updateBothPage: Call = controllers.triage.routes.UpdateBothController.onPageLoad()

  private def sessionExpiredPage: Call = controllers.routes.SessionExpiredController.onPageLoad()


  private def dataDescriber(answers: UserAnswers): String = answers.toString
}


