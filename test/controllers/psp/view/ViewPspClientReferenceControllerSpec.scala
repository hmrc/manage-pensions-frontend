/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers.psp.view

import connectors.FakeUserAnswersCacheConnector
import controllers.ControllerSpecBase
import controllers.actions._
import controllers.psp.view.routes._
import forms.invitations.psp.PspClientReferenceFormProvider
import identifiers.invitations.psp.PspNameId
import identifiers.psp.deauthorise.PspDetailsId
import identifiers.{SchemeNameId, SchemeSrnId}
import models.{CheckMode, SchemeReferenceNumber}
import play.api.data.Form
import play.api.mvc.Call
import play.api.test.Helpers.{contentAsString, redirectLocation, status, _}
import testhelpers.CommonBuilders.{pspDetails, pspDetails2}
import utils.UserAnswers
import views.html.invitations.psp.pspClientReference


class ViewPspClientReferenceControllerSpec extends ControllerSpecBase {
  val formProvider = new PspClientReferenceFormProvider()
  val form: Form[String] = formProvider()

  def onwardRoute: Call = Call("GET", "/foo")

  private val schemeName = "Test Scheme"
  private val srn = "srn"
  private val userAnswer = UserAnswers()
    .set(PspNameId)("test-psp-name").asOpt.value
    .set(SchemeNameId)(schemeName).asOpt.value
    .set(SchemeSrnId)(srn).asOpt.value
  val userAnswerWithPspClientRef: UserAnswers = userAnswer.set(PspDetailsId(0))(pspDetails).asOpt.value

  val minimalData = new FakeDataRetrievalAction(Some(userAnswer.json))

  private def returnCall: Call = controllers.psa.routes.PsaSchemeDashboardController.onPageLoad(SchemeReferenceNumber("srn"))

  private def onSubmitCall: Call = ViewPspClientReferenceController.onSubmit(CheckMode, 0)


  private val view = injector.instanceOf[pspClientReference]

  def controller(dataRetrievalAction: DataRetrievalAction = minimalData) = new ViewPspClientReferenceController(
    messagesApi, FakeAuthAction, FakeUserAnswersCacheConnector,
    dataRetrievalAction, new DataRequiredActionImpl, formProvider, controllerComponents, view
  )

  private def viewAsString(form: Form[_]): String =
    view(form, "test-psp-name", CheckMode, schemeName, returnCall, onSubmitCall)(fakeRequest, messages).toString

  "ViewPspClientReferenceController" when {
    "on a GET" must {

      "return OK and the correct view" in {
        val data = new FakeDataRetrievalAction(Some(userAnswerWithPspClientRef.json))
        val result = controller(data).onPageLoad(CheckMode, 0)(fakeRequest)
        contentAsString(result) mustBe viewAsString(form.fill("A0000000"))
      }

      "redirect to the session expired page if there is no psp name" in {
        val result = controller(getEmptyData).onPageLoad(CheckMode, 0)(fakeRequest)
        redirectLocation(result).value mustBe controllers.routes.SessionExpiredController.onPageLoad.url
      }

      "redirect to the session expired page if not authorisingPSA" in {
        val userAnswerWithPspClient: UserAnswers = userAnswer.set(PspDetailsId(0))(pspDetails2).asOpt.value
        val data = new FakeDataRetrievalAction(Some(userAnswerWithPspClient.json))
        val result = controller(data).onPageLoad(CheckMode, 0)(fakeRequest)
        redirectLocation(result).value mustBe controllers.routes.SessionExpiredController.onPageLoad.url
      }

      "redirect to the session expired page if there is no existing data" in {
        val result = controller(dontGetAnyData).onPageLoad(CheckMode, 0)(fakeRequest)
        redirectLocation(result).value mustBe controllers.routes.SessionExpiredController.onPageLoad.url
      }
    }

    "on a POST" must {
      "save the data and redirect to the next page if valid data is submitted" in {
        val postRequest = fakeRequest.withFormUrlEncodedBody(("reference", "A0000000"))
        val data = new FakeDataRetrievalAction(Some(userAnswerWithPspClientRef.json))
        val result = controller(data).onSubmit(CheckMode, 0)(postRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(ViewPspCheckYourAnswersController.onPageLoad(0).url)
      }

      "return a Bad Request and errors if invalid data is submitted" in {
        val postRequest = fakeRequest.withFormUrlEncodedBody(("reference", ""))
        val data = new FakeDataRetrievalAction(Some(userAnswerWithPspClientRef.json))
        val boundForm = form.bind(Map("reference" -> ""))

        val result = controller(data).onSubmit(CheckMode, 0)(postRequest)

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe viewAsString(boundForm)
      }

      "redirect to the session expired page if there is no psp name" in {
        val result = controller(getEmptyData).onSubmit(CheckMode, 0)(fakeRequest)
        redirectLocation(result).value mustBe controllers.routes.SessionExpiredController.onPageLoad.url
      }
      "redirect to the session expired page if not authorisingPSA" in {
        val postRequest = fakeRequest.withFormUrlEncodedBody(("reference", "123"))
        val userAnswerWithPspClient: UserAnswers = userAnswer.set(PspDetailsId(0))(pspDetails2).asOpt.value
        val data = new FakeDataRetrievalAction(Some(userAnswerWithPspClient.json))
        val result = controller(data).onSubmit(CheckMode, 0)(postRequest)
        redirectLocation(result).value mustBe controllers.routes.SessionExpiredController.onPageLoad.url
      }

      "redirect to the session expired page if there is no existing data" in {
        val result = controller(dontGetAnyData).onSubmit(CheckMode, 0)(fakeRequest)
        redirectLocation(result).value mustBe controllers.routes.SessionExpiredController.onPageLoad.url
      }
    }
  }
}

