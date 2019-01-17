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

package controllers.deregister

import connectors.{DeregistrationConnector, FakeUserAnswersCacheConnector, TaxEnrolmentsConnector, UserAnswersCacheConnector}
import controllers.ControllerSpecBase
import controllers.actions._
import forms.deregister.ConfirmStopBeingPsaFormProvider
import identifiers.invitations.PSANameId
import play.api.data.Form
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsFormUrlEncoded
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.countryOptions.CountryOptions
import views.html.deregister.confirmStopBeingPsa

import scala.concurrent.{ExecutionContext, Future}

class ConfirmStopBeingPsaControllerSpec extends ControllerSpecBase{

  import ConfirmStopBeingPsaControllerSpec._

  "ConfirmStopBeingPsaController" must {

    "return to session expired if psaName is not present" in {
      val result = controller().onPageLoad()(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.SessionExpiredController.onPageLoad().url)
    }

    "return OK and the correct view for a GET" in {
      val result = controller(testData).onPageLoad()(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString()
    }

    "return to session expired if psaName is not present for Post" in {
      val result = controller().onSubmit()(postRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.SessionExpiredController.onPageLoad().url)
    }

    "should display the errors if no selection made" in {

      val result = controller(testData).onSubmit()(fakeRequest)

      status(result) mustBe BAD_REQUEST
    }

    "redirect to the next page on a successful POST when selected true" in {

      val result = controller(testData).onSubmit()(postRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.deregister.routes.SuccessfulDeregistrationController.onPageLoad().url)
    }

    "redirect to the next page on a successful POST when selected false" in {

      val result = controller(testData).onSubmit()(postRequestCancle)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(psaDetailsUrl)
    }
  }

}

object ConfirmStopBeingPsaControllerSpec extends ControllerSpecBase {

  private def psaDetailsUrl = frontendAppConfig.registeredPsaDetailsUrl

  private val formProvider = new ConfirmStopBeingPsaFormProvider
  private val form: Form[Boolean] = formProvider()

  private val postRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
    FakeRequest().withFormUrlEncodedBody(("value", "true"))

  private val postRequestCancle: FakeRequest[AnyContentAsFormUrlEncoded] =
    FakeRequest().withFormUrlEncodedBody(("value", "false"))

  private val countryOptions = new CountryOptions(environment, frontendAppConfig)


  private def testData = new FakeDataRetrievalAction(Some(Json.obj(PSANameId.toString -> "psaName")))


  private def fakeTaxEnrolmentsConnector: TaxEnrolmentsConnector = new TaxEnrolmentsConnector{
    override def deEnrol(groupId: String, enrolmentKey: String)(
      implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = Future.successful(HttpResponse(NO_CONTENT))
  }

  private def fakeDeregistrationConnector: DeregistrationConnector = new DeregistrationConnector{
    override def stopBeingPSA(psaId: String)(
      implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = Future.successful(HttpResponse(NO_CONTENT))
  }

  private def controller(
                          dataRetrievalAction: DataRetrievalAction = getEmptyData,
                          dataCacheConnector: UserAnswersCacheConnector = FakeUserAnswersCacheConnector
                        ) =
    new ConfirmStopBeingPsaController(
      frontendAppConfig,
      FakeAuthAction(),
      messagesApi,
      formProvider,
      dataRetrievalAction,
      new DataRequiredActionImpl,
      fakeDeregistrationConnector,
      fakeTaxEnrolmentsConnector
    )

  private def viewAsString(): String =
    confirmStopBeingPsa(frontendAppConfig, form, "psaName")(fakeRequest, messages).toString

}


