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

package utils

import base.SpecBase
import controllers.invitations.psa.routes._
import models.{Address, CheckMode}
import org.scalatest.MustMatchers
import utils.countryOptions.CountryOptions
import viewmodels.AnswerRow

class CheckYourAnswersHelperSpec
  extends SpecBase
    with MustMatchers {

  val userAnswers = UserAnswers()

  private val countryOptions = new CountryOptions(environment, frontendAppConfig)

  def getHelper(userAnswers: UserAnswers = userAnswers) = new CheckYourAnswersHelper(userAnswers, countryOptions)

  "calling psaName" must {

    "return none if data is not present" in {

      getHelper(userAnswers).psaName mustBe None
    }

    "return answer row if data present" in {

      getHelper(userAnswers.inviteeName("abc")).psaName mustBe Some(
        AnswerRow(
          label = "messages__check__your__answer__psa__name__label",
          answer = Seq("abc"),
          answerIsMessageKey = true,
          changeUrl = Some(PsaNameController.onPageLoad(CheckMode).url)
        )
      )
    }
  }

  "calling psaId" must {

    "return none if data is not present" in {

      getHelper(userAnswers).psaId mustBe None
    }

    "return answer row if data present" in {

      getHelper(userAnswers.inviteeId("A0000000")).psaId mustBe Some(
        AnswerRow(
          label = "messages__check__your__answer__psa__id__label",
          answer = Seq("A0000000"),
          answerIsMessageKey = true,
          changeUrl = Some(PsaIdController.onPageLoad(CheckMode).url)
        )
      )
    }
  }

  "calling addressAnswer" must {
    "return the address as a sequence" in {
      val address = Address.apply("addr1", "addr2", Some("addr3"), Some("addr4"), Some("zz11zz"), "GB")
      val expectedResult = Seq("addr1", "addr2", "addr3", "addr4", "zz11zz", "United Kingdom")
      getHelper(userAnswers).addressAnswer(address) mustBe expectedResult
    }
  }
}
