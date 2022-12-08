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

import connectors.UserAnswersCacheConnector
import controllers.psa.routes._
import controllers.psp.deauthorise.routes._
import controllers.routes._
import identifiers.psp.deauthorise._
import identifiers.{Identifier, SchemeSrnId}
import models.Index
import play.api.mvc.Call
import utils.{Navigator, UserAnswers}

import javax.inject.{Inject, Singleton}

@Singleton
class DeauthorisePSPNavigator @Inject()(val dataCacheConnector: UserAnswersCacheConnector)
  extends Navigator {

  override def routeMap(ua: UserAnswers): PartialFunction[Identifier, Call] = {
    case ConfirmDeauthorisePspId(index) =>
      confirmDeauthPspRoutes(ua, index)
    case PspDeauthDateId(index) =>
      PsaDeauthPspDeclarationController.onPageLoad(index)
    case PsaDeauthorisePspDeclarationId(index) =>
      ConfirmPsaDeauthPspController.onPageLoad(index)
  }

  private def confirmDeauthPspRoutes(userAnswers: UserAnswers, index: Index): Call = {
    (userAnswers.get(ConfirmDeauthorisePspId(index)), userAnswers.get(SchemeSrnId)) match {
      case (Some(false), Some(srn)) =>
        PsaSchemeDashboardController.onPageLoad(srn)
      case (Some(true), _) =>
        PspDeauthDateController.onPageLoad(index)
      case _ =>
        SessionExpiredController.onPageLoad
    }
  }

  override protected def editRouteMap(ua: UserAnswers): PartialFunction[Identifier, Call] = {
    case _ =>
      SessionExpiredController.onPageLoad
  }
}
