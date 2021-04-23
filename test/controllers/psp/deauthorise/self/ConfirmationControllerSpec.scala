/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers.psp.deauthorise.self

import connectors.FakeUserAnswersCacheConnector
import connectors.admin.MinimalConnector
import controllers.ControllerSpecBase
import controllers.actions._
import identifiers.{AuthorisedPractitionerId, SchemeNameId}
import models.MinimalPSAPSP
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.test.Helpers._
import testhelpers.CommonBuilders._
import uk.gov.hmrc.domain.PspId
import views.html.psp.deauthorisation.self.confirmation

import scala.concurrent.Future

class ConfirmationControllerSpec extends ControllerSpecBase with MockitoSugar {

  private val schemeName = "test-scheme"
  private val email = "test@email.com"
  private val pspId = Some(PspId("00000000"))
  private val data = Json.obj(SchemeNameId.toString -> schemeName,
    AuthorisedPractitionerId.toString -> pspDetails)

  private val view = injector.instanceOf[confirmation]
  private val mockMinimalConnector = mock[MinimalConnector]

  def controller(dataRetrievalAction: DataRetrievalAction = new FakeDataRetrievalAction(Some(data), pspId = pspId)) =
    new ConfirmationController(messagesApi, FakeAuthAction, dataRetrievalAction, new DataRequiredActionImpl,
      mockMinimalConnector, FakeUserAnswersCacheConnector, controllerComponents, view)

  private def viewAsString = view(schemeName, psaName, email)(fakeRequest, messages).toString

  "Confirmation Controller" when {
    "on a GET" must {

      "return OK and the correct view" in {
        when(mockMinimalConnector.getMinimalPspDetails(any())(any(), any()))
          .thenReturn(Future.successful(MinimalPSAPSP(email, isPsaSuspended = false, None, None, rlsFlag = false, deceasedFlag = false)))
        val result = controller().onPageLoad()(fakeRequest)
        status(result) mustBe OK
        contentAsString(result) mustBe viewAsString
      }

      "redirect to the session expired page if there is no required data" in {
        val result = controller(getEmptyData).onPageLoad()(fakeRequest)
        redirectLocation(result).value mustBe controllers.routes.SessionExpiredController.onPageLoad().url
      }
    }
  }

}
