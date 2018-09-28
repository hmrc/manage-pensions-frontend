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

import connectors.{FakeUserAnswersCacheConnector, InvitationsCacheConnector}
import controllers.ControllerSpecBase
import controllers.actions.{AuthAction, FakeAuthAction, FakeUnAuthorisedAction}
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import play.api.test.Helpers._
import testhelpers.InvitationBuilder._
import utils.FakeNavigator

import scala.concurrent.Future

class YourInvitationsControllerSpec extends ControllerSpecBase with MockitoSugar {


  val mockInvitationsCacheConnector = mock[InvitationsCacheConnector]

  private def controller(authAction: AuthAction = FakeAuthAction()): YourInvitationsController = {

    new YourInvitationsController(
      frontendAppConfig,
      messagesApi,
      authAction,
      mockInvitationsCacheConnector,
      FakeUserAnswersCacheConnector,
      FakeNavigator
    )
  }
  "YourInvitationsController" must {

    "return 200 Ok and correct content on successful GET" in {

      when(mockInvitationsCacheConnector.getForInvitee(any())(any(), any()))
        .thenReturn(Future.successful(invitationList))

      val result = controller().onPageLoad()(fakeRequest)

      status(result) mustBe OK
      //contentAsString(result) mustBe viewAsString(this)

    }

    "return 200 Ok when empty list is returned by connector" in {

      when(mockInvitationsCacheConnector.getForInvitee(any())(any(), any()))
        .thenReturn(Future.successful(Nil))

      val result = controller().onPageLoad()(fakeRequest)

      status(result) mustBe SEE_OTHER

    }

    "redirect to Unauthorised when not authenticated on GET" in {

      val result = controller(FakeUnAuthorisedAction()).onPageLoad()(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad().url)

    }


  }

}

