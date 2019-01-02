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

import base.SpecBase
import connectors.{FakeUserAnswersCacheConnector, MinimalPsaConnector, SchemeDetailsConnector}
import controllers.actions.{FakeAuthAction, FakeUnAuthorisedAction}
import identifiers.MinimalSchemeDetailId
import models._
import org.scalatest.mockito.MockitoSugar
import play.api.test.Helpers._
import testhelpers.CommonBuilders
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class InviteControllerSpec extends SpecBase with MockitoSugar {

  import InviteControllerSpec._

  def fakeMinimalPsaConnector(isSuspended: Boolean): MinimalPsaConnector = new MinimalPsaConnector {
    override def getMinimalPsaDetails(psaId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[MinimalPSA] =
      Future.successful(psaMinimalSubscription.copy(isPsaSuspended = isSuspended))
  }

  def fakeSchemeDetailsConnector: SchemeDetailsConnector = new SchemeDetailsConnector {
    override def getSchemeDetails(psaId: String, schemeIdType: String,
                                  idNumber: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[PsaSchemeDetails] =
      Future.successful(CommonBuilders.psaSchemeDetailsResponse)
  }

  def controller(isSuspended: Boolean) = new InviteController(mockAuthAction, fakeSchemeDetailsConnector,
    FakeUserAnswersCacheConnector, fakeMinimalPsaConnector(isSuspended))


  "InviteController calling onPageLoad(srn)" must {

    "redirect to you cannot send an invite page if PSASuspension is true" in {

      val result = controller(isSuspended = true).onPageLoad(srn)(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.invitations.routes.YouCannotSendAnInviteController.onPageLoad().url)

      FakeUserAnswersCacheConnector.verifyNot(MinimalSchemeDetailId)
    }

    "save minimal scheme details and then redirect to psa name page if PSASuspension is false" in {
      val result = controller(isSuspended = false).onPageLoad(srn)(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.invitations.routes.PsaNameController.onPageLoad(NormalMode).url)

      FakeUserAnswersCacheConnector.verify(MinimalSchemeDetailId, MinimalSchemeDetail(srn, Some(pstr), schemeName))
    }

    "redirect to unauthorised page if user is not authenticated" in {

      val controller = new InviteController(FakeUnAuthorisedAction(), fakeSchemeDetailsConnector,
        FakeUserAnswersCacheConnector, fakeMinimalPsaConnector(isSuspended = false))

      val result = controller.onPageLoad(srn)(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad.url)
    }
  }
}

object InviteControllerSpec {
  private val email = "test@test.com"
  val srn = "S9000000000"
  val pstr = "00000000AA"
  val schemeName = "Benefits Scheme"

  private val psaMinimalSubscription = MinimalPSA(email, false, None, Some(IndividualDetails("First", Some("Middle"), "Last")))
  private val mockAuthAction = FakeAuthAction()
}
