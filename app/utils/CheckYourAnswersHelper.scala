/*
 * Copyright 2018 HM Revenue & Customs
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

package utils

import identifiers.SchemeDetailId
import identifiers.invitations.{PSAId, PsaNameId}
import models.{CheckMode, MinimalSchemeDetail}
import viewmodels.AnswerRow



class CheckYourAnswersHelper(userAnswers: UserAnswers) extends Enumerable.Implicits {

  def psaName: Option[AnswerRow] = {
    userAnswers.get(PsaNameId) map { answer =>
      AnswerRow("messages__check__your__answer__psa__name__label", Seq(answer), true, Some(controllers.invitations.routes.PsaNameController.onPageLoad(CheckMode).url))
    }
  }

  def psaId: Option[AnswerRow] = {
    userAnswers.get(PSAId) map { answer =>
      AnswerRow("messages__check__your__answer__psa__id__label", Seq(answer), true, Some(controllers.invitations.routes.PsaIdController.onPageLoad(CheckMode).url))
    }
  }

}
