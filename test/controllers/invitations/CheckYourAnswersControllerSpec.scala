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

package controllers.invitations

import controllers.ControllerSpecBase
import controllers.actions.{DataRequiredActionImpl, DataRetrievalAction, FakeAuthAction}
import models.MinimalSchemeDetail
import play.api.mvc.Call
import play.api.test.Helpers._
import utils.{UserAnswers, CheckYourAnswersFactory}
import viewmodels.AnswerSection
import views.html.check_your_answers

class CheckYourAnswersControllerSpec extends ControllerSpecBase {

  val checkYourAnswersFactory = new CheckYourAnswersFactory()

  def call: Call = controllers.invitations.routes.CheckYourAnswersController.onSubmit()

  val testSrn: String = "test-srn"
  val testInviteeName = "test-invitee-name"
  val testPstr = "test-pstr"
  val testSchemeName = "test-scheme-name"
  val testSchemeDetail = MinimalSchemeDetail(testSrn, Some(testPstr), testSchemeName)

  val userAnswer = UserAnswers().minimalSchemeDetails(testSchemeDetail)

  def controller(dataRetrievalAction: DataRetrievalAction = getEmptyData) =
    new CheckYourAnswersController(
      frontendAppConfig,
      messagesApi,
      FakeAuthAction(),
      dataRetrievalAction,
      new DataRequiredActionImpl,
      checkYourAnswersFactory
    )

  "Check Your Answers Controller" must {

    "return 200 and the correct view for a GET" in {
      val result = controller(userAnswer.dataRetrievalAction).onPageLoad()(fakeRequest)

      status(result) mustBe OK

      val expectedViewContent = check_your_answers(frontendAppConfig, Seq(AnswerSection(None, Seq())), None, call,
        Some("messages__check__your__answer__main__containt__label"), Some(testSchemeName))(fakeRequest, messages).toString

      contentAsString(result) mustBe expectedViewContent
    }

    "redirect to Session Expired for a GET if not existing data is found" in {
      val result = controller(dontGetAnyData).onPageLoad()(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.SessionExpiredController.onPageLoad().url)
    }

  }
}
