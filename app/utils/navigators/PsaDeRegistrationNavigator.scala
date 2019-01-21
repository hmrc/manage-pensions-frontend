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

import config.FrontendAppConfig
import connectors.UserAnswersCacheConnector
import identifiers.deregister.ConfirmStopBeingPsaId
import javax.inject.Inject
import utils.{Navigator, UserAnswers}
import play.api.mvc.Call

class PsaDeRegistrationNavigator @Inject()(val dataCacheConnector: UserAnswersCacheConnector, config: FrontendAppConfig) extends Navigator {

  override def routeMap(from: NavigateFrom): Option[NavigateTo] = from.id match {
    case ConfirmStopBeingPsaId => deregisterRoutes(from.userAnswers)
    case _ => None
  }

  override protected def editRouteMap(from: NavigateFrom): Option[NavigateTo] = from.id match { case _ => None }

  private def deregisterRoutes(answers: UserAnswers): Option[NavigateTo] = answers.get(ConfirmStopBeingPsaId) match {
    case Some(true) => NavigateTo.dontSave(controllers.routes.SessionExpiredController.onPageLoad())
    case Some(false) => NavigateTo.dontSave(new Call("GET",config.registeredPsaDetailsUrl))
    case _ => NavigateTo.dontSave(controllers.routes.SessionExpiredController.onPageLoad())
  }
}