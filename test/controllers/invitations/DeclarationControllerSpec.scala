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
import forms.invitations.DeclarationFormProvider
import identifiers.invitations.DeclarationId
import play.api.data.Form
import play.api.mvc.Call
import play.api.test.Helpers._
import utils._
import views.html.invitations.declaration

class DeclarationControllerSpec extends ControllerSpecBase {

  import DeclarationControllerSpec._

  def controller(dataRetrievalAction: DataRetrievalAction = getEmptyData) = new DeclarationController(
    frontendAppConfig,
    messagesApi,
    formProvider,
    FakeAuthAction(),
    dataRetrievalAction,
    new DataRequiredActionImpl,
    FakeDataCacheConnector,
    new FakeNavigator(onwardRoute)
  )

  private def viewAsString(form: Form[_] = form) = declaration(frontendAppConfig, hasAdviser, isMasterTrust, form)(fakeRequest, messages).toString

  "Declaration Controller" when {

    "on a GET" must {

      "return OK and the correct view" in {
        val result = controller(data).onPageLoad()(fakeRequest)

        status(result) mustBe OK
        contentAsString(result) mustBe viewAsString()
      }

      "redirect to Session Expired page if there is no user answers for HaveYouEmployedPensionAdviser" in {
        val data = new FakeDataRetrievalAction(Some(UserAnswers().isMasterTrust(isMasterTrust).json))
        val result = controller(data).onPageLoad()(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe sessionExpired
      }

      "redirect to Session Expired page if there is no user answers for isMasterTrust" in {
        val data = new FakeDataRetrievalAction(Some(UserAnswers().havePensionAdviser(hasAdviser).json))
        val result = controller(data).onPageLoad()(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe sessionExpired
      }

      "redirect to Session Expired page if there is no cached data" in {
        val result = controller(dontGetAnyData).onPageLoad()(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe sessionExpired
      }
    }

    "on a POST" must {

      "save the answer and redirect to next page when valid data is submitted" in {
        val result = controller(data).onSubmit()(fakeRequest.withFormUrlEncodedBody("agree" -> "agreed"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe onwardRoute.url
        FakeDataCacheConnector.verify(DeclarationId, true)
      }

      "return Bad Request if invalid data is submitted" in {
        val formWithErrors = form.withError("agree", messages("messages__error__declaration__required"))
        val result = controller(data).onSubmit()(fakeRequest)
        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe viewAsString(formWithErrors)
      }

      "redirect to Session Expired page if there is no cached data" in {
        val result = controller(dontGetAnyData).onSubmit()(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe sessionExpired
      }

      "redirect to Session Expired page if there is no user answers for HaveYouEmployedPensionAdviser for an invalid request" in {
        val data = new FakeDataRetrievalAction(Some(UserAnswers().isMasterTrust(isMasterTrust).json))
        val result = controller(data).onSubmit()(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe sessionExpired
      }

      "redirect to Session Expired page if there is no user answers for isMasterTrust for an invalid request" in {
        val data = new FakeDataRetrievalAction(Some(UserAnswers().havePensionAdviser(hasAdviser).json))
        val result = controller(data).onSubmit()(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe sessionExpired
      }
    }
  }
}

object DeclarationControllerSpec {
  val hasAdviser = true
  val isMasterTrust = false

  def onwardRoute = Call("GET", "/foo")

  val data = new FakeDataRetrievalAction(Some(UserAnswers().
    havePensionAdviser(hasAdviser).
    isMasterTrust(isMasterTrust).json
  ))

  val formProvider = new DeclarationFormProvider()
  val form = formProvider()

  private def sessionExpired = controllers.routes.SessionExpiredController.onPageLoad().url
}