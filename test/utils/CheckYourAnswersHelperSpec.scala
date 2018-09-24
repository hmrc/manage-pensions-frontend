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

import models.CheckMode
import org.scalatest.{MustMatchers, WordSpec}
import viewmodels.AnswerRow

class CheckYourAnswersHelperSpec extends WordSpec with MustMatchers {

  val userAnswers = UserAnswers()

  def getHelper(userAnswers : UserAnswers = userAnswers) = new CheckYourAnswersHelper(userAnswers)

  "calling psaName" must {

    "return none if data is not present" in {

      getHelper(userAnswers).psaName mustBe None
    }

    "return answer row if data present" in {

      getHelper(userAnswers.inviteeName("abc")).psaName mustBe Some(
        AnswerRow("messages__check__your__answer__psa__name__label", Seq("abc"), true, Some(controllers.invitations.routes.PsaNameController.onPageLoad(CheckMode).url)))
    }
  }

  "calling psaId" must {

    "return none if data is not present" in {

      getHelper(userAnswers).psaId mustBe None
    }

    "return answer row if data present" in {

      getHelper(userAnswers.inviteeId("A0000000")).psaId mustBe Some(
        AnswerRow("messages__check__your__answer__psa__id__label", Seq("A0000000"), true, Some(controllers.invitations.routes.PsaIdController.onPageLoad(CheckMode).url)))
    }
  }

}
