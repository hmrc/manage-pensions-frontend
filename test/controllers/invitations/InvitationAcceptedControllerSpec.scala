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

package controllers.invitations

import connectors.FakeUserAnswersCacheConnector
import controllers.ControllerSpecBase
import controllers.actions._
import models.MinimalSchemeDetail
import play.api.test.Helpers._
import utils.UserAnswers
import views.html.invitations.invitationAccepted

class InvitationAcceptedControllerSpec extends ControllerSpecBase {

  val testSchemeName: String = "Test Scheme Name"
  val getRelevantData: DataRetrievalAction = UserAnswers().schemeName(testSchemeName).dataRetrievalAction

  def controller(authAction: AuthAction = FakeAuthAction(), dataRetrievalAction: DataRetrievalAction = getRelevantData):
  InvitationAcceptedController =
    new InvitationAcceptedController(
      frontendAppConfig,
      messagesApi,
      authAction,
      dataRetrievalAction,
      new DataRequiredActionImpl,
      FakeUserAnswersCacheConnector
    )

  def viewAsString(): String = invitationAccepted(frontendAppConfig, testSchemeName)(fakeRequest, messages).toString

  "InvitationAccepted Controller" must {

    "return OK with correct content on GET" in {
      val result = controller().onPageLoad(fakeRequest)
      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString()
      FakeUserAnswersCacheConnector.verifyAllDataRemoved()
    }

    "redirect to session expired if required data is missing" in {

      val result = controller(dataRetrievalAction = getEmptyData).onPageLoad(fakeRequest)
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.SessionExpiredController.onPageLoad().url)
    }

    "redirect to unauthorised if user action is not authenticated" in {

      val result = controller(authAction = FakeUnAuthorisedAction()).onPageLoad(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad().url)
    }
  }
}
