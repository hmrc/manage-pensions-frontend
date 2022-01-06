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

package controllers.invitations

import connectors.FakeUserAnswersCacheConnector
import connectors.InvitationsCacheConnector
import controllers.ControllerSpecBase
import controllers.actions._
import identifiers.SchemeSrnId
import identifiers.psa.PSANameId
import models.NormalMode
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.mvc.Call
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import testhelpers.InvitationBuilder._
import utils.FakeNavigator
import views.html.invitations.yourInvitations

import scala.concurrent.Future

class YourInvitationsControllerSpec extends ControllerSpecBase with MockitoSugar {

  private val nextCall = Call("GET", "www.example.com")
  private val psaName = "Test Psa Name"
  private val navigator = new FakeNavigator(nextCall, NormalMode)

  private val mockInvitationsCacheConnector = mock[InvitationsCacheConnector]

  val dataRetrievalAction = new FakeDataRetrievalAction(Some(Json.obj(
    PSANameId.toString -> "Test Psa Name"
  )))

  private val yourInvitationsView = injector.instanceOf[yourInvitations]

  private def controller(authAction: AuthAction = FakeAuthAction): YourInvitationsController = {

    new YourInvitationsController(
      frontendAppConfig,
      messagesApi,
      authAction,
      dataRetrievalAction,
      new DataRequiredActionImpl,
      mockInvitationsCacheConnector,
      FakeUserAnswersCacheConnector,
      navigator,
      controllerComponents,
      yourInvitationsView
    )
  }

  private def viewAsString: () => HtmlFormat.Appendable = () => yourInvitationsView(invitationList, psaName)(fakeRequest, messages)

  "YourInvitationsController" must {

    "return 200 Ok and correct content on successful GET" in {

      when(mockInvitationsCacheConnector.getForInvitee(any())(any(), any()))
        .thenReturn(Future.successful(invitationList))

      val result = controller().onPageLoad()(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString().toString()

    }

    "redirect to Unauthorised when not authenticated on GET" in {

      val result = controller(FakeUnAuthorisedAction).onPageLoad()(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad().url)

    }

    "redirect to the next page when valid data is submitted" in {

      val result = controller().onSelect(srn)(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(nextCall.url)
      FakeUserAnswersCacheConnector.verifyAllDataRemoved()
      FakeUserAnswersCacheConnector.verify(SchemeSrnId, srn.id)
    }
  }

}

