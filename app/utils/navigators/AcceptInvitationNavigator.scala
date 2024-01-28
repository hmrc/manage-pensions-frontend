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

import controllers.invitations.psa.routes._
import controllers.invitations.routes._
import identifiers.invitations._
import identifiers.invitations.psa._
import identifiers.{Identifier, SchemeSrnId}
import models.NormalMode
import play.api.mvc.Call
import utils.{Navigator, UserAnswers}

import javax.inject.{Inject, Singleton}

@Singleton
class AcceptInvitationNavigator @Inject() extends Navigator {

  //scalastyle:off cyclomatic.complexity
  override def routeMap(ua: UserAnswers): PartialFunction[Identifier, Call] = {
    case SchemeSrnId =>
      DoYouHaveWorkingKnowledgeController.onPageLoad(NormalMode)
    case DoYouHaveWorkingKnowledgeId =>
      doYouHaveWorkingKnowledgeRoutes(ua)
    case AdviserNameId =>
      AdviserEmailAddressController.onPageLoad(NormalMode)
    case AdviserEmailId =>
      AdviserAddressPostcodeLookupController.onPageLoad()
    case AdviserAddressPostCodeLookupId =>
      PensionAdviserAddressListController.onPageLoad(NormalMode)
    case AdviserAddressListId =>
      AdviserManualAddressController.onPageLoad(NormalMode, prepopulated = true)
    case AdviserAddressId =>
      CheckPensionAdviserAnswersController.onPageLoad()
    case CheckPensionAdviserAnswersId =>
      DeclarationController.onPageLoad()
    case DeclarationId =>
      InvitationAcceptedController.onPageLoad()
  }

  //scalastyle:on cyclomatic.complexity

  private def doYouHaveWorkingKnowledgeRoutes(userAnswers: UserAnswers): Call = {
    userAnswers.get(DoYouHaveWorkingKnowledgeId) match {
      case Some(false) =>
        AdviserDetailsController.onPageLoad(NormalMode)
      case Some(true) =>
        DeclarationController.onPageLoad()
      case _ =>
        controllers.routes.SessionExpiredController.onPageLoad
    }
  }

  override protected def editRouteMap(ua: UserAnswers): PartialFunction[Identifier, Call] = {
    case AdviserNameId | AdviserEmailId | AdviserAddressId =>
      CheckPensionAdviserAnswersController.onPageLoad()
  }
}
