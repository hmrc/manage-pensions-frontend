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

import connectors.UserAnswersCacheConnector
import controllers.routes._
import identifiers.remove.{ConfirmRemovePspId, PspRemovalDateId}
import identifiers.{Identifier, SchemeSrnId}
import javax.inject.{Inject, Singleton}
import models.Index
import play.api.mvc.Call
import utils.{Navigator, UserAnswers}

@Singleton
class RemovePSPNavigator @Inject()(val dataCacheConnector: UserAnswersCacheConnector) extends Navigator {

  override def routeMap(ua: UserAnswers): PartialFunction[Identifier, Call] = {
    case ConfirmRemovePspId(index) => confirmRemovePspRoutes(ua, index)
    case PspRemovalDateId(_) => controllers.psp.routes.ViewPractitionersController.onPageLoad()
  }

  private def confirmRemovePspRoutes(userAnswers: UserAnswers, index: Index): Call = {
    (userAnswers.get(ConfirmRemovePspId(index)), userAnswers.get(SchemeSrnId)) match {
      case (Some(false), Some(srn)) =>
        controllers.routes.SchemeDetailsController.onPageLoad(srn)
      case (Some(true), _) =>
        controllers.remove.routes.PspRemovalDateController.onPageLoad(index)
      case _ =>
        controllers.routes.SessionExpiredController.onPageLoad()
    }
  }

  override protected def editRouteMap(ua: UserAnswers): PartialFunction[Identifier, Call] = {
    case _ => SessionExpiredController.onPageLoad()
  }
}
