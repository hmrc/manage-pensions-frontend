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

import connectors.UserAnswersCacheConnector
import identifiers.SchemeSrnId
import identifiers.invitations._
import javax.inject.{Inject, Singleton}
import models.NormalMode
import utils.{Navigator, UserAnswers}

@Singleton
class AcceptInvitationNavigator @Inject()(val dataCacheConnector: UserAnswersCacheConnector) extends Navigator {

  //scalastyle:off cyclomatic.complexity
  override def routeMap(from: NavigateFrom): Option[NavigateTo] = from.id match {
    case SchemeSrnId =>
      NavigateTo.dontSave(controllers.invitations.routes.DoYouHaveWorkingKnowledgeController.onPageLoad(NormalMode))
    case DoYouHaveWorkingKnowledgeId =>
      doYouHaveWorkingKnowledgeRoutes(from.userAnswers)
    case AdviserNameId =>
      NavigateTo.dontSave(controllers.invitations.routes.AdviserEmailAddressController.onPageLoad(NormalMode))
    case AdviserEmailId =>
      NavigateTo.dontSave(controllers.invitations.routes.AdviserAddressPostcodeLookupController.onPageLoad())
    case AdviserAddressPostCodeLookupId =>
      NavigateTo.dontSave(controllers.invitations.routes.PensionAdviserAddressListController.onPageLoad(NormalMode))
    case AdviserAddressListId =>
      NavigateTo.dontSave(controllers.invitations.routes.AdviserManualAddressController.onPageLoad(NormalMode, prepopulated = true))
    case AdviserAddressId =>
      NavigateTo.dontSave(controllers.invitations.routes.CheckPensionAdviserAnswersController.onPageLoad())
    case CheckPensionAdviserAnswersId =>
      NavigateTo.dontSave(controllers.invitations.routes.DeclarationController.onPageLoad())
    case DeclarationId =>
      NavigateTo.dontSave(controllers.invitations.routes.InvitationAcceptedController.onPageLoad())
    case _ =>
      NavigateTo.dontSave(controllers.routes.SessionExpiredController.onPageLoad())
  }
  //scalastyle:on cyclomatic.complexity

  private def doYouHaveWorkingKnowledgeRoutes(userAnswers: UserAnswers) = {
    userAnswers.get(DoYouHaveWorkingKnowledgeId) match {
      case Some(false) =>
        NavigateTo.dontSave(controllers.invitations.routes.AdviserDetailsController.onPageLoad(NormalMode))
      case Some(true) =>
        NavigateTo.dontSave(controllers.invitations.routes.DeclarationController.onPageLoad())
      case _ =>
        NavigateTo.dontSave(controllers.routes.SessionExpiredController.onPageLoad())
    }
  }

  override protected def editRouteMap(from: NavigateFrom): Option[NavigateTo] = from.id match {
    case AdviserNameId | AdviserEmailId | AdviserAddressId =>
      NavigateTo.dontSave(controllers.invitations.routes.CheckPensionAdviserAnswersController.onPageLoad())
    case _ => NavigateTo.dontSave(controllers.routes.SessionExpiredController.onPageLoad())
  }
}
