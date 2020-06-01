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

import config.FrontendAppConfig
import controllers.routes._
import identifiers.Identifier
import identifiers.triage._
import javax.inject.{Inject, Singleton}
import models.triage.DoesPSAStartWithATwo.{No, StartWithA2AndA0, Yes}
import models.triage.WhatDoYouWantToDo._
import play.api.mvc.Call
import utils.{Enumerable, Navigator, UserAnswers}

@Singleton
class TriageNavigator @Inject()(appConfig: FrontendAppConfig) extends Navigator with Enumerable.Implicits {

  override def routeMap(ua: UserAnswers): PartialFunction[Identifier, Call] = {
    case WhatDoYouWantToDoId => whatDoYouWantToDoRoutes(ua)
    case DoesPSTRStartWithTwoId => doesPSTRStartWithTwoRoutes(ua)
    case DoesPSTRStartWithTwoInviteId => doesPSTRStartWithTwoInviteRoutes(ua)
    case DoesPSTRStartWithTwoInvitedId => doesPSTRStartWithTwoInvitedRoutes(ua)
    case DoesPSTRStartWithTwoUpdateId => doesPSTRStartWithTwoUpdateRoutes(ua)
    case DoesPSAStartWithATwoId => doesPSAStartWithATwoRoutes(ua)
  }

  private def whatDoYouWantToDoRoutes(ua: UserAnswers): Call = {
    ua.get(WhatDoYouWantToDoId) match {
      case Some(ManageExistingScheme) => controllers.triage.routes.DoesPSTRStartWithTwoController.onPageLoad()
      case Some(CheckTheSchemeStatus) => Call("GET", s"${appConfig.loginUrl}?continue=${appConfig.loginToListSchemesUrl}")
      case Some(Invite) => controllers.triage.routes.DoesPSTRStartWithTwoInviteController.onPageLoad()
      case Some(BecomeAnAdmin) => controllers.triage.routes.DoesPSTRStartWithTwoInvitedController.onPageLoad()
      case Some(UpdateSchemeInformation) => controllers.triage.routes.DoesPSTRStartWithTwoUpdateController.onPageLoad()
      case Some(ChangeAdminDetails) => controllers.triage.routes.DoesPSAStartWithATwoController.onPageLoad()
      case _ => controllers.routes.SessionExpiredController.onPageLoad()
    }
  }

  private def doesPSAStartWithATwoRoutes(ua: UserAnswers): Call = {
    ua.get(DoesPSAStartWithATwoId) match {
      case Some(Yes) => Call("GET", s"${appConfig.loginUrl}?continue=${appConfig.registeredPsaDetailsUrl}")
      case Some(No) => Call("GET", appConfig.tpssInitialQuestionsUrl)
      case Some(StartWithA2AndA0) => controllers.triage.routes.ATwoAndAZeroIdsController.onPageLoad()
      case _ => controllers.routes.SessionExpiredController.onPageLoad()
    }
  }

  private def doesPSTRStartWithTwoRoutes(answers: UserAnswers): Call = {
    answers.get(DoesPSTRStartWithTwoId) match {
      case Some(true) => Call("GET", s"${appConfig.loginUrl}?continue=${appConfig.loginToListSchemesUrl}")
      case Some(false) => Call("GET", appConfig.tpssWelcomeUrl)
      case _ => controllers.routes.SessionExpiredController.onPageLoad()
    }
  }

  private def doesPSTRStartWithTwoInviteRoutes(answers: UserAnswers): Call = {
    answers.get(DoesPSTRStartWithTwoInviteId) match {
      case Some(true) => controllers.triage.routes.InvitingPSTRStartWithTwoController.onPageLoad()
      case Some(false) => Call("GET", appConfig.pensionSchemesInvitationGuideGovUkLink)
      case _ => controllers.routes.SessionExpiredController.onPageLoad()
    }
  }

  private def doesPSTRStartWithTwoInvitedRoutes(answers: UserAnswers): Call = {
    answers.get(DoesPSTRStartWithTwoInvitedId) match {
      case Some(true) => Call("GET", s"${appConfig.loginUrl}?continue=${appConfig.loginToListSchemesUrl}")
      case Some(false) => Call("GET", appConfig.pensionSchemesAddToSchemeGuideGovUkLink)
      case _ => controllers.routes.SessionExpiredController.onPageLoad()
    }
  }

  private def doesPSTRStartWithTwoUpdateRoutes(answers: UserAnswers): Call = {
    answers.get(DoesPSTRStartWithTwoUpdateId) match {
      case Some(true) => controllers.triage.routes.UpdatingPSTRStartWithTwoController.onPageLoad()
      case Some(false) => Call("GET", appConfig.pensionSchemesGuideGovUkLink)
      case _ => controllers.routes.SessionExpiredController.onPageLoad()
    }
  }

  override protected def editRouteMap(ua: UserAnswers): PartialFunction[Identifier, Call] = {
    case _ => SessionExpiredController.onPageLoad()
  }
}
