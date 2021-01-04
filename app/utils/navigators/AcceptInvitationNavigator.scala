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

import identifiers.Identifier
import identifiers.SchemeSrnId
import identifiers.invitations._
import javax.inject.Inject
import javax.inject.Singleton
import models.NormalMode
import play.api.mvc.Call
import utils.Navigator
import utils.UserAnswers

@Singleton
class AcceptInvitationNavigator @Inject() extends Navigator {

  //scalastyle:off cyclomatic.complexity
  override def routeMap(ua: UserAnswers): PartialFunction[Identifier, Call] = {
    case SchemeSrnId =>
      controllers.invitations.routes.DoYouHaveWorkingKnowledgeController.onPageLoad(NormalMode)
    case DoYouHaveWorkingKnowledgeId =>
      doYouHaveWorkingKnowledgeRoutes(ua)
    case AdviserNameId =>
      controllers.invitations.routes.AdviserEmailAddressController.onPageLoad(NormalMode)
    case AdviserEmailId =>
      controllers.invitations.routes.AdviserAddressPostcodeLookupController.onPageLoad()
    case AdviserAddressPostCodeLookupId =>
      controllers.invitations.routes.PensionAdviserAddressListController.onPageLoad(NormalMode)
    case AdviserAddressListId =>
      controllers.invitations.routes.AdviserManualAddressController.onPageLoad(NormalMode, prepopulated = true)
    case AdviserAddressId =>
      controllers.invitations.routes.CheckPensionAdviserAnswersController.onPageLoad()
    case CheckPensionAdviserAnswersId =>
      controllers.invitations.routes.DeclarationController.onPageLoad()
    case DeclarationId =>
      controllers.invitations.routes.InvitationAcceptedController.onPageLoad()
  }

  //scalastyle:on cyclomatic.complexity

  private def doYouHaveWorkingKnowledgeRoutes(userAnswers: UserAnswers): Call = {
    userAnswers.get(DoYouHaveWorkingKnowledgeId) match {
      case Some(false) =>
        controllers.invitations.routes.AdviserDetailsController.onPageLoad(NormalMode)
      case Some(true) =>
        controllers.invitations.routes.DeclarationController.onPageLoad()
      case _ =>
        controllers.routes.SessionExpiredController.onPageLoad()
    }
  }

  override protected def editRouteMap(ua: UserAnswers): PartialFunction[Identifier, Call] = {
    case AdviserNameId | AdviserEmailId | AdviserAddressId =>
      controllers.invitations.routes.CheckPensionAdviserAnswersController.onPageLoad()
  }
}
