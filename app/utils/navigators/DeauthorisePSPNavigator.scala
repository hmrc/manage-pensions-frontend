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

import connectors.UserAnswersCacheConnector
import controllers.psa.routes._
import controllers.psp.deauthorise.routes._
import controllers.routes._
import identifiers.psp.deauthorise._
import identifiers.{Identifier, SchemeSrnId}
import models.{Index, SchemeReferenceNumber}
import play.api.mvc.Call
import utils.{Navigator, UserAnswers}

import javax.inject.{Inject, Singleton}

@Singleton
class DeauthorisePSPNavigator @Inject()(val dataCacheConnector: UserAnswersCacheConnector)
  extends Navigator {

  override def routeMap(ua: UserAnswers, srn: SchemeReferenceNumber): PartialFunction[Identifier, Call] = {
    case ConfirmDeauthorisePspId(index) =>
      confirmDeauthPspRoutes(ua, index, srn)
    case PspDeauthDateId(index) =>
      PsaDeauthPspDeclarationController.onPageLoad(index, srn)
    case PsaDeauthorisePspDeclarationId(index) =>
      ConfirmPsaDeauthPspController.onPageLoad(index, srn)
  }

  private def confirmDeauthPspRoutes(userAnswers: UserAnswers, index: Index, srn: SchemeReferenceNumber): Call = {
    (userAnswers.get(ConfirmDeauthorisePspId(index)), userAnswers.get(SchemeSrnId)) match {
      case (Some(false), Some(uaSrn)) =>
        PsaSchemeDashboardController.onPageLoad(uaSrn)
      case (Some(true), _) =>
        PspDeauthDateController.onPageLoad(index, srn)
      case _ =>
        SessionExpiredController.onPageLoad
    }
  }

  override protected def editRouteMap(ua: UserAnswers, srn: SchemeReferenceNumber): PartialFunction[Identifier, Call] = {
    case _ =>
      SessionExpiredController.onPageLoad
  }
}
