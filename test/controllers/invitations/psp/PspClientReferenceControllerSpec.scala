/*
 * Copyright 2020 HM Revenue & Customs
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
import forms.invitations.psp.PspClientReferenceFormProvider
import identifiers.{SchemeNameId, SchemeSrnId}
import identifiers.invitations.psp.{PspClientReferenceId, PspNameId}
import models.{NormalMode, SchemeReferenceNumber}
import models.invitations.psp.ClientReference._
import play.api.data.Form
import play.api.mvc.Call
import play.api.test.Helpers.{contentAsString, redirectLocation, status, _}
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import utils.{FakeNavigator, UserAnswers}
import views.html.invitations.psp.pspClientReference


class PspClientReferenceControllerSpec extends ControllerSpecBase {
  val formProvider = new PspClientReferenceFormProvider()
  val form = formProvider()

  def onwardRoute: Call = Call("GET", "/foo")
  private val schemeName = "Test Scheme"
  private val srn = "srn"
  private val userAnswer = UserAnswers()
    .set(PspNameId)("xyz").asOpt.value
    .set(SchemeNameId)(schemeName).asOpt.value
    .set(SchemeSrnId)(srn).asOpt.value
  val userAnswerWithPspClientRef: UserAnswers = userAnswer.set(PspClientReferenceId)(HaveClientReference("A0000000")).asOpt.value
  val minimalData = new FakeDataRetrievalAction(Some(userAnswer.json))

  private val returnCall = controllers.routes.SchemeDetailsController.onPageLoad(SchemeReferenceNumber("srn"))


  private val view = injector.instanceOf[pspClientReference]

  def controller(dataRetrievalAction: DataRetrievalAction = minimalData) = new PspClientReferenceController(
    frontendAppConfig, messagesApi, FakeAuthAction(), new FakeNavigator(onwardRoute), FakeUserAnswersCacheConnector,
    dataRetrievalAction, new DataRequiredActionImpl, formProvider, stubMessagesControllerComponents(), view
  )

  private def viewAsString(form: Form[_] = form): String = view(form, "xyz", NormalMode, schemeName, returnCall)(fakeRequest, messages).toString

  "PspClientReferenceController" when {
    "on a GET" must {

      "return OK and the correct view" in {
        val result = controller().onPageLoad(NormalMode)(fakeRequest)
        status(result) mustBe OK
        contentAsString(result) mustBe viewAsString()
      }

      "populate the view correctly on a GET if the question has previously been answered" in {
        val data = new FakeDataRetrievalAction(Some(userAnswerWithPspClientRef.json))
        val result = controller(data).onPageLoad(NormalMode)(fakeRequest)
        contentAsString(result) mustBe viewAsString(form.fill(HaveClientReference("A0000000")))
      }

      "redirect to the session expired page if there is no psp name" in {
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
        val postRequest = fakeRequest.withFormUrlEncodedBody(("value.yesNo", "true"), ("value.reference", "A0000000"))

        val result = controller().onSubmit(NormalMode)(postRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(onwardRoute.url)
      }

      "return a Bad Request and errors if invalid data is submitted" in {
        val postRequest = fakeRequest.withFormUrlEncodedBody(("value.yesNo", ""))
        val boundForm = form.bind(Map("value.yesNo" -> ""))

        val result = controller().onSubmit(NormalMode)(postRequest)

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe viewAsString(boundForm)
      }

      "redirect to the session expired page if there is no psp name" in {
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

