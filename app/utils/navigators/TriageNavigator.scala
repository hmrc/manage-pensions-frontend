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

import config.FrontendAppConfig
import controllers.triage.routes._
import identifiers.Identifier
import identifiers.triage._

import javax.inject.Inject
import javax.inject.Singleton
import models.triage.DoesPSAStartWithATwo.No
import models.triage.DoesPSAStartWithATwo.Yes
import models.triage.{DoesPSTRStartWithATwo, WhatDoYouWantToDo}
import models.triage.WhatDoYouWantToDo._
import models.triage.WhatRole.{PSA, PSP}
import play.api.mvc.Call
import utils.Enumerable
import utils.Navigator
import utils.UserAnswers

@Singleton
class TriageNavigator @Inject()(appConfig: FrontendAppConfig) extends Navigator with Enumerable.Implicits {

  override def routeMap(ua: UserAnswers): PartialFunction[Identifier, Call] = {
    case WhatRoleId => whatRoleRoutes(ua)
    case WhatDoYouWantToDoId => whatDoYouWantToDoRoutes(ua)
    case DoesPSTRStartWithTwoId => doesPSTRStartWithTwoRoutes(ua)
    case DoesPSTRStartWithTwoInviteId => doesPSTRStartWithTwoInviteRoutes(ua)
    case DoesPSTRStartWithTwoInvitedId => doesPSTRStartWithTwoInvitedRoutes(ua)
    case DoesPSTRStartWithTwoAuthPspId => doesPSTRStartWithTwoAuthPspRoutes(ua)
    case DoesPSTRStartWithTwoUpdateId => doesPSTRStartWithTwoUpdateRoutes(ua)
    case DoesPSAStartWithATwoId => doesPSAStartWithATwoRoutes(ua)
  }

  private def whatRoleRoutes(ua: UserAnswers): Call =
    ua.get(WhatRoleId) match {
      case role@(Some(PSA) | Some(PSP)) => WhatDoYouWantToDoController.onPageLoad(role.get.toString)
      case _ => NotRegisteredController.onPageLoad()
    }

  private def whatDoYouWantToDoRoutes(ua: UserAnswers): Call =
    ua.get(WhatRoleId) match {
      case role@(Some(PSA) | Some(PSP)) =>
          ua.get(WhatDoYouWantToDoId)(reads(WhatDoYouWantToDo.enumerable(role.get.toString))) match {
            case Some(ManageExistingScheme) => DoesPSTRStartWithTwoController.onPageLoad(role.get.toString)
            case Some(CheckTheSchemeStatus) => Call("GET", s"${appConfig.loginUrl}?continue=${appConfig.loginToListSchemesUrl}")
            case Some(Invite) => DoesPSTRStartWithTwoInviteController.onPageLoad()
            case Some(BecomeAnAdmin) => DoesPSTRStartWithTwoInvitedController.onPageLoad()
            case Some(AuthorisePsp) => DoesPSTRStartWithTwoAuthPspController.onPageLoad()
            case Some(UpdateSchemeInformation) => DoesPSTRStartWithTwoUpdateController.onPageLoad()
            case Some(ChangeAdminDetails) => DoesPSAStartWithATwoController.onPageLoad()
            case Some(RegisterScheme) => Call("GET", appConfig.registerSchemeGuideGovUkLink)
            case Some(ChangePspDetails) => DoesPSAStartWithATwoController.onPageLoad()
            case Some(DeauthYourself) => DoesPSAStartWithATwoController.onPageLoad()
            case _ => controllers.routes.SessionExpiredController.onPageLoad()
          }
      case _ => controllers.routes.SessionExpiredController.onPageLoad()
  }

  private def doesPSAStartWithATwoRoutes(ua: UserAnswers): Call = {
    ua.get(DoesPSAStartWithATwoId) match {
      case Some(Yes) => Call("GET", s"${appConfig.loginUrl}?continue=${appConfig.registeredPsaDetailsUrl}")
      case Some(No) => UpdateBothController.onPageLoad()
      case _ => controllers.routes.SessionExpiredController.onPageLoad()
    }
  }

  private def doesPSTRStartWithTwoRoutes(ua: UserAnswers): Call = {
    (ua.get(DoesPSTRStartWithTwoId), ua.get(WhatRoleId)) match {
      case (Some(DoesPSTRStartWithATwo.Yes), Some(PSA)) => Call("GET", s"${appConfig.loginUrl}?continue=${appConfig.loginToListSchemesUrl}")
      case (Some(DoesPSTRStartWithATwo.Yes), Some(PSP)) => Call("GET", s"${appConfig.loginUrl}?continue=${appConfig.loginToListSchemesPspUrl}")
      case (Some(DoesPSTRStartWithATwo.No), _) => Call("GET", appConfig.tpssWelcomeUrl)
      case _ => controllers.routes.SessionExpiredController.onPageLoad()
    }
  }

  private def doesPSTRStartWithTwoInviteRoutes(answers: UserAnswers): Call = {
    answers.get(DoesPSTRStartWithTwoInviteId) match {
      case Some(DoesPSTRStartWithATwo.Yes) => InvitingPSTRStartWithTwoController.onPageLoad()
      case Some(DoesPSTRStartWithATwo.No) => Call("GET", appConfig.pensionSchemesInvitationGuideGovUkLink)
      case _ => controllers.routes.SessionExpiredController.onPageLoad()
    }
  }

  private def doesPSTRStartWithTwoInvitedRoutes(answers: UserAnswers): Call = {
    answers.get(DoesPSTRStartWithTwoInvitedId) match {
      case Some(DoesPSTRStartWithATwo.Yes) => Call("GET", s"${appConfig.loginUrl}?continue=${appConfig.loginToListSchemesUrl}")
      case Some(DoesPSTRStartWithATwo.No) => Call("GET", appConfig.pensionSchemesAddToSchemeGuideGovUkLink)
      case _ => controllers.routes.SessionExpiredController.onPageLoad()
    }
  }

  private def doesPSTRStartWithTwoAuthPspRoutes(answers: UserAnswers): Call = {
    answers.get(DoesPSTRStartWithTwoAuthPspId) match {
      case Some(DoesPSTRStartWithATwo.Yes) => AuthorisePractitionerController.onPageLoad()
      case Some(DoesPSTRStartWithATwo.No) => Call("GET", appConfig.authorisePractitionerGuideGovUkLink)
      case _ => controllers.routes.SessionExpiredController.onPageLoad()
    }
  }

  private def doesPSTRStartWithTwoUpdateRoutes(answers: UserAnswers): Call = {
    answers.get(DoesPSTRStartWithTwoUpdateId) match {
      case Some(DoesPSTRStartWithATwo.Yes) => UpdatingPSTRStartWithTwoController.onPageLoad()
      case Some(DoesPSTRStartWithATwo.No) => Call("GET", appConfig.pensionSchemesGuideMandatoryOnlineFilingGovUkLink)
      case _ => controllers.routes.SessionExpiredController.onPageLoad()
    }
  }

  override protected def editRouteMap(ua: UserAnswers): PartialFunction[Identifier, Call] = {
    case _ => controllers.routes.SessionExpiredController.onPageLoad()
  }
}
