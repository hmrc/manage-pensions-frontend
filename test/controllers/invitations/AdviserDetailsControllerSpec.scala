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

import connectors.FakeDataCacheConnector
import controllers.ControllerSpecBase
import controllers.actions.{DataRequiredActionImpl, DataRetrievalAction, FakeAuthAction, FakeDataRetrievalAction}
import forms.invitations.AdviserDetailsFormProvider
import identifiers.invitations.AdviserNameId
import models.NormalMode
import play.api.data.Form
import play.api.libs.json.Json
import play.api.mvc.Call
import play.api.test.Helpers._
import utils.FakeNavigator
import views.html.invitations.adviserDetails

class AdviserDetailsControllerSpec extends ControllerSpecBase {

  val formProvider = new AdviserDetailsFormProvider()
  val form = formProvider()

  def onwardRoute = Call("GET", "/foo")

  def controller(dataRetrievalAction: DataRetrievalAction = getEmptyData) = new AdviserDetailsController(
    frontendAppConfig, messagesApi, FakeAuthAction(), new FakeNavigator(onwardRoute), dataRetrievalAction, new DataRequiredActionImpl, formProvider,
    FakeDataCacheConnector
  )

  private def viewAsString(form: Form[_] = form) = adviserDetails(frontendAppConfig, form, NormalMode)(fakeRequest, messages).toString

  "AdviserDetailsController" when {
    "on a GET" must {

      "return OK and the correct view" in {
        val result = controller().onPageLoad(NormalMode)(fakeRequest)
        status(result) mustBe OK
        contentAsString(result) mustBe viewAsString()
      }

      "populate the view correctly on a GET when the question has previously been answered" in {
        val data = new FakeDataRetrievalAction(Some(Json.obj(AdviserNameId.toString -> "test")))
        val result = controller(data).onPageLoad(NormalMode)(fakeRequest)
        contentAsString(result) mustBe viewAsString(form.fill("test"))
      }

      "redirect to the session expired page when there is no user answers" in {
        val result = controller(dontGetAnyData).onPageLoad(NormalMode)(fakeRequest)
        redirectLocation(result).value mustBe controllers.routes.SessionExpiredController.onPageLoad().url
      }
    }

    "on a POST" must {
      "redirect to the next page when valid data is submitted" in {
        val postRequest = fakeRequest.withFormUrlEncodedBody((AdviserNameId.toString, "answer"))

        val result = controller().onSubmit(NormalMode)(postRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(onwardRoute.url)
      }

      "return a Bad Request and errors when invalid data is submitted" in {
        val postRequest = fakeRequest.withFormUrlEncodedBody((AdviserNameId.toString, ""))
        val boundForm = form.bind(Map(AdviserNameId.toString -> ""))

        val result = controller().onSubmit(NormalMode)(postRequest)

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe viewAsString(boundForm)
      }

      "redirect to the session expired page when there is no user answers" in {
        val result = controller(dontGetAnyData).onSubmit(NormalMode)(fakeRequest)
        redirectLocation(result).value mustBe controllers.routes.SessionExpiredController.onPageLoad().url
      }
    }
  }

}
