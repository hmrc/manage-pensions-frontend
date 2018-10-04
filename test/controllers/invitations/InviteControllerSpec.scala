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

import base.SpecBase
import connectors.{FakeUserAnswersCacheConnector, MinimalPsaConnector, SchemeDetailsConnector}
import controllers.actions.{FakeAuthAction, FakeUnAuthorisedAction}
import models._
import org.mockito.Matchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.BeforeAndAfter
import org.scalatest.mockito.MockitoSugar
import play.api.test.Helpers._
import testhelpers.CommonBuilders._

import scala.concurrent.Future

class InviteControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfter{

  import InviteControllerSpec._

  val srn = "test-srn"
  val mockMinimalPsaConnector = mock[MinimalPsaConnector]
  val mockSchemeDetailsConnector = mock[SchemeDetailsConnector]
  val controller = new InviteController(mockAuthAction, mockSchemeDetailsConnector, FakeUserAnswersCacheConnector, mockMinimalPsaConnector)

  before{
    reset(mockMinimalPsaConnector)
  }

  "InviteController calling onPageLoad(srn)" must {

    "return 303 if PSASuspension is false" in {

      when(mockMinimalPsaConnector.getMinimalPsaDetails(any())(any(), any())).thenReturn(Future.successful(psaMinimalSubscription))

      val result = controller.onPageLoad(srn)(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.invitations.routes.PsaNameController.onPageLoad(srn)(NormalMode).url)
      verify(mockMinimalPsaConnector,  times(1)).getMinimalPsaDetails(any())(any(), any())

    }

    "return 303 if PSASuspension is true" in {

      when(mockMinimalPsaConnector.getMinimalPsaDetails(any())(any(), any())).thenReturn(Future.successful(
        psaMinimalSubscription.copy(isPsaSuspended = true)))

      val result = controller.onPageLoad(srn)(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.invitations.routes.YouCannotSendAnInviteController.onPageLoad(srn)().url)
      verify(mockMinimalPsaConnector,  times(1)).getMinimalPsaDetails(any())(any(), any())

    }

    "return 303 if request is unauthorised" in {

      val controller = new InviteController(FakeUnAuthorisedAction(), mockSchemeDetailsConnector, FakeUserAnswersCacheConnector, mockMinimalPsaConnector)
      val result = controller.onPageLoad(srn)(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad(srn).url)
      verify(mockMinimalPsaConnector,  times(0)).getMinimalPsaDetails(any())(any(), any())

    }
  }
}

object InviteControllerSpec {
  private val email = "test@test.com"

  private val psaMinimalSubscription = MinimalPSA(email,false,None,Some(IndividualDetails("First",Some("Middle"),"Last")))

  private val mockAuthAction =  FakeAuthAction()
}
