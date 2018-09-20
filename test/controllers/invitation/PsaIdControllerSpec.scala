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

package controllers.invitation

import connectors.FakeDataCacheConnector
import controllers.ControllerSpecBase
import controllers.actions._
import forms.invitation.PsaIdFromProvider
import identifiers.{PSAId, PsaNameId}
import models.NormalMode
import play.api.data.Form
import play.api.libs.json.Json
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.{UserAnswers, FakeNavigator}
import views.html.invitation.psaId

class PsaIdControllerSpec extends ControllerSpecBase {

  val formProvider = new PsaIdFromProvider()
  val form = formProvider()

  val navigator =  new FakeNavigator(onwardRoute)

  val userAnswer = UserAnswers().set(PsaNameId)("xyz").asOpt.value
  val userAnswerWithPsaID = userAnswer.set(PSAId)("A0000000").asOpt.value
  val data = getDataRetrieval(userAnswer)

  def onwardRoute = Call("GET", "/foo")

  def getDataRetrieval(userAnswer: UserAnswers) = {
    new FakeDataRetrievalAction(Some(userAnswer.json))
  }

  def controller(dataRetrievalAction: DataRetrievalAction = getEmptyData) = new PsaIdController(
    frontendAppConfig, messagesApi, FakeAuthAction(), navigator, FakeDataCacheConnector,
    dataRetrievalAction, new DataRequiredActionImpl, formProvider)

  def viewAsString(form: Form[_] = form) = psaId(frontendAppConfig, form, "xyz", NormalMode)(fakeRequest, messages).toString

  "PsaIdController calling onPageLoad" must {

    "return OK and the correct view for a GET" in {

      val result =  controller(data).onPageLoad(NormalMode)(FakeRequest())

      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString()
    }

    "populate the view correctly on a GET when the question has previously been answered" in {

      val data = getDataRetrieval(userAnswerWithPsaID)
      val result =  controller(data).onPageLoad(NormalMode)(FakeRequest())

      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString(form.fill("A0000000"))

    }

    "redirect to Session Expired if no existing data is found" in {

      val result =  controller().onPageLoad(NormalMode)(FakeRequest())

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.SessionExpiredController.onPageLoad().url)
    }

    "return 303 if user action is not authenticated" in {

      val controller =  new PsaIdController(
        frontendAppConfig, messagesApi, FakeUnAuthorisedAction(), navigator, FakeDataCacheConnector,
        getEmptyData, new DataRequiredActionImpl, formProvider)

      val result =  controller.onPageLoad(NormalMode)(FakeRequest())

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad.url)
    }

  }

  "PsaIdController calling onSubmit" must {

    "return a Bad Request and errors when invalid data is submitted" in {

      val boundForm = form.bind(Map("psaId" -> ""))
      val result = controller(data).onSubmit(NormalMode)(fakeRequest)

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe viewAsString(boundForm)
    }

    "redirect to next screen of valid data is present" in {

      val result = controller(data).onSubmit(NormalMode)(FakeRequest().withJsonBody(userAnswerWithPsaID.json))

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(onwardRoute.url)
    }

    "redirect to Session Expired if no existing data is found" in {

      val result =  controller().onPageLoad(NormalMode)(FakeRequest())

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.SessionExpiredController.onPageLoad().url)
    }

  }

}