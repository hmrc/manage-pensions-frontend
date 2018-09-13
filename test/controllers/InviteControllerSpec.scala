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

package controllers

import base.SpecBase
import connectors.SubscriptionConnector
import controllers.actions.AuthAction
import models._
import models.requests.AuthenticatedRequest
import org.scalatest.BeforeAndAfter
import org.scalatest.mockito.MockitoSugar
import org.mockito.Matchers.any
import org.mockito.Mockito.{reset, when, verify, times}
import play.api.mvc.{Result, Request}
import play.api.test.Helpers._
import uk.gov.hmrc.domain.PsaId

import scala.concurrent.Future

class InviteControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfter{

  import InviteControllerSpec._

  val mockConnector = mock[SubscriptionConnector]
  val controller = new InviteController(mockAuthAction, mockConnector)

  before(reset(mockConnector))

  "InviteController calling onPageLoad" must {

    "return 200 if PSASuspension is false" in {

      when(mockConnector.getSubscriptionDetails(any())(any(), any())).thenReturn(Future.successful(SubscriptionDetails(psaSubscription)))

      val result = controller.onPageLoad(fakeRequest)

      status(result) mustBe OK
      verify(mockConnector,  times(1)).getSubscriptionDetails(any())(any(), any())

    }

    "return 303 if PSASuspension is true" in {

      when(mockConnector.getSubscriptionDetails(any())(any(), any())).thenReturn(Future.successful(
        SubscriptionDetails(psaSubscription.copy(isPSASuspension = true))))

      val result = controller.onPageLoad(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.YouCannotSendAnInviteController.onPageLoad().url)
      verify(mockConnector,  times(1)).getSubscriptionDetails(any())(any(), any())

    }

  }

}

object InviteControllerSpec {

  private val address = Address(false, "Telford1", "Telford2", Some("Telford13"), Some("Telford14"), Some("TF3 4ER"), "GB")
  private val contactDetails = ContactDetails("0044-09876542312", Some("0044-09876542312"), Some("0044-09876542312"), "abc@hmrc.gsi.gov.uk")
  private val indEstPrevAdd = PreviousAddressDetails(true, Some(Address(true,"sddsfsfsdf","sddsfsdf",Some("sdfdsfsdf"),Some("sfdsfsdf"),Some("456546"),"AD")))
  private val customerIdentificationDetails = CustomerIdentificationDetails(legalStatus="AA", None, None, noIdentifier=false)
  private val declarationDetails = PensionSchemeAdministratorDeclaration(true, true, true, true, Some(true), Some(true), true, None)

  private val psaSubscription = PsaSubscriptionDetails(isPSASuspension=false,
    customerIdentificationDetails=customerIdentificationDetails,
    organisationOrPartnerDetails=None,
    individualDetails=None,
    correspondenceAddressDetails= address,
    correspondenceContactDetails = contactDetails,
    previousAddressDetails= indEstPrevAdd,
    numberOfDirectorsOrPartnersDetails=None,
    directorOrPartnerDetails=None,
    declarationDetails = declarationDetails)

  private val psaId = "A1234567"

  private val mockAuthAction =  new AuthAction {
    override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {
      block(AuthenticatedRequest(request, "test-external-id", PsaId(psaId)))
    }
  }

}