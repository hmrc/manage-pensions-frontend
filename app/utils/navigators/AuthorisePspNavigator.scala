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
import identifiers.Identifier
import identifiers.invitations.psp._
import models.{CheckMode, Mode, NormalMode, SchemeReferenceNumber}
import play.api.mvc.Call
import utils.{Navigator, UserAnswers}

import javax.inject.{Inject, Singleton}

@Singleton
class AuthorisePspNavigator @Inject() extends Navigator {

  override def routeMap(ua: UserAnswers, srn: SchemeReferenceNumber): PartialFunction[Identifier, Call] = {
    case PspNameId => PspIdController.onPageLoad(NormalMode, srn)
    case PspId => PspHasClientReferenceController.onPageLoad(NormalMode, srn)
    case PspHasClientReferenceId => pspHasClientReferenceRoutes(ua, NormalMode, srn)
    case PspClientReferenceId => CheckYourAnswersController.onPageLoad(srn)
  }

  override protected def editRouteMap(ua: UserAnswers, srn: SchemeReferenceNumber): PartialFunction[Identifier, Call] = {
    case PspNameId => CheckYourAnswersController.onPageLoad(srn)
    case PspId => CheckYourAnswersController.onPageLoad(srn)
    case PspHasClientReferenceId => pspHasClientReferenceRoutes(ua, CheckMode, srn)
    case PspClientReferenceId => CheckYourAnswersController.onPageLoad(srn)
  }

  private def pspHasClientReferenceRoutes(userAnswers: UserAnswers, mode: Mode, srn: SchemeReferenceNumber): Call = {
    userAnswers.get(PspHasClientReferenceId) match {
      case Some(true) =>
        PspClientReferenceController.onPageLoad(mode, srn)
      case Some(false) =>
        CheckYourAnswersController.onPageLoad(srn)
      case _ =>
        controllers.routes.SessionExpiredController.onPageLoad
    }
  }
}
