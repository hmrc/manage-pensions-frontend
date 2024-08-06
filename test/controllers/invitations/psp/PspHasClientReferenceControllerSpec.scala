/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.invitations.psp

import connectors.FakeUserAnswersCacheConnector
import controllers.ControllerSpecBase
import controllers.actions._
import controllers.invitations.psp.routes._
import controllers.psa.routes._
import forms.invitations.psp.PspHasClientReferenceFormProvider
import identifiers.invitations.psp.{PspHasClientReferenceId, PspNameId}
import identifiers.{SchemeNameId, SchemeSrnId}
import models.{NormalMode, SchemeReferenceNumber}
import play.api.data.Form
import play.api.mvc.Call
import play.api.test.Helpers.{contentAsString, redirectLocation, status, _}
import utils.{FakeNavigator, UserAnswers}
import views.html.invitations.psp.pspHasClientReference


class PspHasClientReferenceControllerSpec extends ControllerSpecBase {
  val formProvider = new PspHasClientReferenceFormProvider()
  val form: Form[Boolean] = formProvider()

  def onwardRoute: Call = Call("GET", "/foo")
  private val schemeName = "Test Scheme"
  private val userAnswer = UserAnswers()
    .set(PspNameId)("xyz").asOpt.value
    .set(SchemeNameId)(schemeName).asOpt.value
    .set(SchemeSrnId)(srn).asOpt.value
  val userAnswerWithPspHasClientRef: UserAnswers = userAnswer.set(PspHasClientReferenceId)(true).asOpt.value
  val minimalData = new FakeDataRetrievalAction(Some(userAnswer.json))

  private def returnCall = PsaSchemeDashboardController.onPageLoad(SchemeReferenceNumber("srn"))
  private def onSubmitCall = PspHasClientReferenceController.onSubmit(NormalMode, srn)


  private val view = injector.instanceOf[pspHasClientReference]

  def controller(dataRetrievalAction: DataRetrievalAction = minimalData) = new PspHasClientReferenceController(
    messagesApi, FakeAuthAction, new FakeNavigator(onwardRoute), FakeUserAnswersCacheConnector,
    dataRetrievalAction, new DataRequiredActionImpl, formProvider, controllerComponents, view, fakePsaSchemeAuthAction
  )

  private def viewAsString(form: Form[_] = form): String = view(form, "xyz", NormalMode, schemeName, returnCall,onSubmitCall)(fakeRequest, messages).toString

  "PspHasClientReferenceController" when {
    "on a GET" must {

      "return OK and the correct view" in {
        val result = controller().onPageLoad(NormalMode, srn)(fakeRequest)
        status(result) mustBe OK
        contentAsString(result) mustBe viewAsString()
      }

      "populate the view correctly on a GET if the question has previously been answered" in {
        val data = new FakeDataRetrievalAction(Some(userAnswerWithPspHasClientRef.json))
        val result = controller(data).onPageLoad(NormalMode, srn)(fakeRequest)
        contentAsString(result) mustBe viewAsString(form.fill(true))
      }

      "redirect to the session expired page if there is no psp name" in {
        val result = controller(getEmptyData).onPageLoad(NormalMode, srn)(fakeRequest)
        redirectLocation(result).value mustBe controllers.routes.SessionExpiredController.onPageLoad.url
      }

      "redirect to the session expired page if there is no existing data" in {
        val result = controller(dontGetAnyData).onPageLoad(NormalMode, srn)(fakeRequest)
        redirectLocation(result).value mustBe controllers.routes.SessionExpiredController.onPageLoad.url
      }
    }

    "on a POST" must {
      "save the data and redirect to the next page if valid data is submitted" in {
        val postRequest = fakeRequest.withFormUrlEncodedBody(("hasReference", "true"))

        val result = controller().onSubmit(NormalMode, srn)(postRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(onwardRoute.url)
      }

      "save the data and redirect to the next page if valid data(False) is submitted" in {
        val postRequest = fakeRequest.withFormUrlEncodedBody(("hasReference", "false"))

        val result = controller().onSubmit(NormalMode, srn)(postRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(onwardRoute.url)
      }

      "return a Bad Request and errors if invalid data is submitted" in {
        val postRequest = fakeRequest.withFormUrlEncodedBody(("hasReference", ""))
        val boundForm = form.bind(Map("hasReference" -> ""))

        val result = controller().onSubmit(NormalMode, srn)(postRequest)

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe viewAsString(boundForm)
      }

      "redirect to the session expired page if there is no psp name" in {
        val result = controller(getEmptyData).onSubmit(NormalMode, srn)(fakeRequest)
        redirectLocation(result).value mustBe controllers.routes.SessionExpiredController.onPageLoad.url
      }

      "redirect to the session expired page if there is no existing data" in {
        val result = controller(dontGetAnyData).onSubmit(NormalMode, srn)(fakeRequest)
        redirectLocation(result).value mustBe controllers.routes.SessionExpiredController.onPageLoad.url
      }
    }
  }
}

