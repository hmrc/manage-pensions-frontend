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
import identifiers.Identifier
import identifiers.deregister.ConfirmStopBeingPsaId
import javax.inject.Inject
import play.api.mvc.Call
import utils.{Navigator, UserAnswers}

class PsaDeRegistrationNavigator @Inject()(config: FrontendAppConfig) extends Navigator {

  override def routeMap(ua: UserAnswers): PartialFunction[Identifier, Call] = {
    case ConfirmStopBeingPsaId => deregisterRoutes(ua)
  }

  override protected def editRouteMap(ua: UserAnswers): PartialFunction[Identifier, Call] = {
    case _ => controllers.routes.IndexController.onPageLoad()
  }

  private def deregisterRoutes(answers: UserAnswers): Call = answers.get(ConfirmStopBeingPsaId) match {
    case Some(true) => controllers.deregister.routes.SuccessfulDeregistrationController.onPageLoad()
    case Some(false) => Call("GET", config.registeredPsaDetailsUrl)
    case _ => controllers.routes.SessionExpiredController.onPageLoad()
  }
}