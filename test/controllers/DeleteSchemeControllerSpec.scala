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

package controllers

import connectors.{MicroserviceCacheConnector, UserAnswersCacheConnector}
import controllers.actions._
import forms.DeleteSchemeFormProvider
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.mockito.MockitoSugar
import play.api.data.Form
import play.api.libs.json.Json
import play.api.mvc.Results.Ok
import play.api.test.Helpers.{contentAsString, _}
import views.html.deleteScheme

import scala.concurrent.Future

class DeleteSchemeControllerSpec extends ControllerSpecBase with MockitoSugar {

  val formProvider = new DeleteSchemeFormProvider()
  val form: Form[Boolean] = formProvider()
  val schemeName = "Test Scheme Name"
  val fakeCacheConnector: UserAnswersCacheConnector = mock[MicroserviceCacheConnector]

  def controller(dataRetrievalAction: DataRetrievalAction = dontGetAnyData): DeleteSchemeController =
    new DeleteSchemeController(frontendAppConfig, messagesApi, fakeCacheConnector, FakeAuthAction(),
      dataRetrievalAction, new DataRequiredActionImpl, formProvider)

  def viewAsString(form: Form[_] = form): String = deleteScheme(frontendAppConfig, form, schemeName)(fakeRequest, messages).toString

  "DeleteScheme Controller" must {

    "return OK and the correct view for a GET" in {
      when(fakeCacheConnector.fetch(eqTo("id"))(any(), any())).thenReturn(Future.successful(Some(Json.obj(
        "schemeName" -> schemeName))))
      val result = controller().onPageLoad(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString()
    }

    "remove all is called to delete user answers when user answers Yes" in {
      when(fakeCacheConnector.fetch(eqTo("id"))(any(), any())).thenReturn(Future.successful(Some(Json.obj(
        "schemeName" -> schemeName))))
      val postRequest = fakeRequest.withFormUrlEncodedBody(("value", "true"))
      when(fakeCacheConnector.removeAll(any())(any(), any())).thenReturn(Future.successful(Ok))

      val result = controller().onSubmit(postRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.SchemesOverviewController.onPageLoad().url)
      verify(fakeCacheConnector, times(1)).removeAll(any())(any(), any())
    }

    "redirect to the overview page when user answers No" in {
      when(fakeCacheConnector.fetch(eqTo("id"))(any(), any())).thenReturn(Future.successful(Some(Json.obj(
        "schemeName" -> schemeName))))
      val postRequest = fakeRequest.withFormUrlEncodedBody(("value", "false"))

      val result = controller().onSubmit(postRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.SchemesOverviewController.onPageLoad().url)
    }

    "return a Bad Request and errors when invalid data is submitted" in {
      val postRequest = fakeRequest.withFormUrlEncodedBody(("value", "invalid value"))
      val boundForm = form.bind(Map("value" -> "invalid value"))

      val result = controller().onSubmit(postRequest)

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe viewAsString(boundForm)
    }

    "redirect to Session Expired for a GET if no existing data is found" in {
      when(fakeCacheConnector.fetch(eqTo("id"))(any(), any())).thenReturn(Future.successful(None))
      val result = controller(dontGetAnyData).onPageLoad(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.SchemesOverviewController.onPageLoad().url)
    }

    "redirect to Session Expired for a POST if no existing data is found" in {
      when(fakeCacheConnector.fetch(eqTo("id"))(any(), any())).thenReturn(Future.successful(None))
      val postRequest = fakeRequest.withFormUrlEncodedBody(("value", "true"))
      val result = controller(dontGetAnyData).onSubmit(postRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.SchemesOverviewController.onPageLoad().url)
    }
  }
}
