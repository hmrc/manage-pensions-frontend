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

import controllers.invitations.psp.routes._
import identifiers.{Identifier, SchemeSrnId}
import identifiers.invitations.psp._
import models.{CheckMode, Mode, NormalMode}
import play.api.mvc.Call
import utils.{Navigator, UserAnswers}

import javax.inject.{Inject, Singleton}

@Singleton
class AuthorisePspNavigator @Inject() extends Navigator {

  override def routeMap(ua: UserAnswers): PartialFunction[Identifier, Call] = {
    case PspNameId => PspIdController.onPageLoad(NormalMode)
    case PspId => PspHasClientReferenceController.onPageLoad(NormalMode)
    case PspHasClientReferenceId => pspHasClientReferenceRoutes(ua, NormalMode)
    case PspClientReferenceId => CheckYourAnswersController.onPageLoad()
  }

  override protected def editRouteMap(ua: UserAnswers): PartialFunction[Identifier, Call] = {
    ua.get(SchemeSrnId) match {
      case Some(srn) => {
        case PspNameId => CheckYourAnswersController.onPageLoad(srn)
        case PspId => CheckYourAnswersController.onPageLoad(srn)
        case PspHasClientReferenceId => pspHasClientReferenceRoutes(ua, CheckMode)
        case PspClientReferenceId => CheckYourAnswersController.onPageLoad(srn)
      }
      case _ => controllers.routes.SessionExpiredController.onPageLoad()
    }
  }

  private def pspHasClientReferenceRoutes(userAnswers: UserAnswers, mode: Mode): Call = {
    userAnswers.get(PspHasClientReferenceId) match {
      case Some(true) =>
        PspClientReferenceController.onPageLoad(mode)
      case Some(false) =>
        CheckYourAnswersController.onPageLoad()
      case _ =>
        controllers.routes.SessionExpiredController.onPageLoad
    }
  }
}
