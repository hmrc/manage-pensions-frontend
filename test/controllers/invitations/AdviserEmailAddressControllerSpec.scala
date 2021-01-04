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

package controllers.invitations

import connectors.FakeUserAnswersCacheConnector
import controllers.ControllerSpecBase
import controllers.actions._
import forms.invitations.AdviserEmailFormProvider
import identifiers.invitations.AdviserEmailId
import identifiers.invitations.AdviserNameId
import models.NormalMode
import play.api.data.Form
import play.api.libs.json.Json
import play.api.mvc.Call
import play.api.test.Helpers._
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import utils.FakeNavigator
import views.html.invitations.adviserEmailAddress

class AdviserEmailAddressControllerSpec extends ControllerSpecBase {

  val formProvider = new AdviserEmailFormProvider()
  val form = formProvider()

  def onwardRoute = Call("GET", "/foo")

  val minimalAdviserData = new FakeDataRetrievalAction(Some(Json.obj(
    AdviserNameId.toString -> "test name"
  )))

  private val view = injector.instanceOf[adviserEmailAddress]

  def controller(dataRetrievalAction: DataRetrievalAction = minimalAdviserData) = new AdviserEmailAddressController(
    frontendAppConfig, messagesApi, FakeAuthAction, new FakeNavigator(onwardRoute), dataRetrievalAction, new DataRequiredActionImpl, formProvider,
    FakeUserAnswersCacheConnector, stubMessagesControllerComponents(), view
  )

  private def viewAsString(form: Form[_] = form) = view(form, NormalMode, "test name")(fakeRequest, messages).toString

  "AdviserDetailsController" when {
    "on a GET" must {

      "return OK and the correct view" in {
        val result = controller().onPageLoad(NormalMode)(fakeRequest)
        status(result) mustBe OK
        contentAsString(result) mustBe viewAsString()
      }

      "populate the view correctly on a GET if the question has previously been answered" in {
        val data = new FakeDataRetrievalAction(Some(Json.obj(AdviserNameId.toString -> "test name", AdviserEmailId.toString -> "test@tes.com")))
        val result = controller(data).onPageLoad(NormalMode)(fakeRequest)
        contentAsString(result) mustBe viewAsString(form.fill("test@tes.com"))
      }

      "redirect to the session expired page if there is no adviser name" in {
        val result = controller(getEmptyData).onPageLoad(NormalMode)(fakeRequest)
        redirectLocation(result).value mustBe controllers.routes.SessionExpiredController.onPageLoad().url
      }

      "redirect to the session expired page if there is no existing data" in {
        val result = controller(dontGetAnyData).onPageLoad(NormalMode)(fakeRequest)
        redirectLocation(result).value mustBe controllers.routes.SessionExpiredController.onPageLoad().url
      }
    }

    "on a POST" must {
      "save the data and redirect to the next page if valid data is submitted" in {
        val postRequest = fakeRequest.withFormUrlEncodedBody(("email", "test@test.com"))

        val result = controller().onSubmit(NormalMode)(postRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(onwardRoute.url)
        FakeUserAnswersCacheConnector.verify(AdviserEmailId, "test@test.com")
      }

      "return a Bad Request and errors if invalid data is submitted" in {
        val postRequest = fakeRequest.withFormUrlEncodedBody(("email", "test@.com"))
        val boundForm = form.bind(Map("email" -> "test@.com"))

        val result = controller().onSubmit(NormalMode)(postRequest)

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe viewAsString(boundForm)
      }

      "redirect to the session expired page if there is no adviser name" in {
        val result = controller(getEmptyData).onSubmit(NormalMode)(fakeRequest)
        redirectLocation(result).value mustBe controllers.routes.SessionExpiredController.onPageLoad().url
      }

      "redirect to the session expired page if there is no existing data" in {
        val result = controller(dontGetAnyData).onSubmit(NormalMode)(fakeRequest)
        redirectLocation(result).value mustBe controllers.routes.SessionExpiredController.onPageLoad().url
      }
    }
  }

}
