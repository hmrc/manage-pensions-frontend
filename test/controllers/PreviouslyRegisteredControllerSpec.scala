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

package controllers

import config.FrontendAppConfig
import controllers.behaviours.ControllerWithQuestionPageBehaviours
import forms.PreviouslyRegisteredFormProvider
import models.{AdministratorOrPractitioner, PreviouslyRegistered}
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.CSRFTokenHelper.addCSRFToken
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.previouslyRegistered

class PreviouslyRegisteredControllerSpec extends ControllerWithQuestionPageBehaviours with ScalaFutures with MockitoSugar with BeforeAndAfterEach {
  private val appConfig: FrontendAppConfig = mock[FrontendAppConfig]
  private val dummyUrl = "/url"

  private val view = injector.instanceOf[previouslyRegistered]
  private val formProvider = new PreviouslyRegisteredFormProvider()

  override def beforeEach(): Unit = {
    reset(appConfig)
  }

  def controller(): PreviouslyRegisteredController =
    new PreviouslyRegisteredController(
      appConfig, messagesApi, formProvider, controllerComponents, view)

  "PreviouslyRegisteredController for administrator" must {
    "return OK with the view when calling on page load" in {
      val request = addCSRFToken(FakeRequest(GET, routes.PreviouslyRegisteredController.onPageLoadAdministrator().url))
      val result = controller().onPageLoadAdministrator(request)

      status(result) mustBe OK
      contentAsString(result) mustBe view(formProvider(), AdministratorOrPractitioner.Administrator)(request, messages).toString
    }

    "return a Bad Request and errors when invalid data is submitted" in {
      val postRequest = FakeRequest(POST, routes.PreviouslyRegisteredController.onSubmitAdministrator().url).withFormUrlEncodedBody("value" -> "invalid value")
      val boundForm = formProvider().bind(Map("value" -> "invalid value"))
      val result = controller().onSubmitAdministrator(postRequest)

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe view(boundForm, AdministratorOrPractitioner.Administrator)(postRequest,messages).toString
    }

    "redirect to the correct next page when yes not logged in chosen (recovery url)" in {
      val postRequest = FakeRequest(POST, routes.PreviouslyRegisteredController.onSubmitAdministrator().url).withFormUrlEncodedBody("value" ->
        PreviouslyRegistered.YesNotLoggedIn.toString)
      when(appConfig.recoverCredentialsPSAUrl).thenReturn(dummyUrl)
      val result = controller().onSubmitAdministrator(postRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe dummyUrl
    }

    "redirect to the correct next page when yes stopped in chosen" in {
      val postRequest = FakeRequest(POST, routes.PreviouslyRegisteredController.onSubmitAdministrator().url).withFormUrlEncodedBody("value" ->
        PreviouslyRegistered.YesStopped.toString)
      when(appConfig.registerSchemeAdministratorUrl).thenReturn(dummyUrl)
      val result = controller().onSubmitAdministrator(postRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe dummyUrl
    }

    "redirect to the correct next page when no chosen" in {
      val postRequest = FakeRequest(POST, routes.PreviouslyRegisteredController.onSubmitAdministrator().url).withFormUrlEncodedBody("value" ->
        PreviouslyRegistered.No.toString)
      when(appConfig.registerSchemeAdministratorUrl).thenReturn(dummyUrl)
      val result = controller().onSubmitAdministrator(postRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe dummyUrl
    }
  }

  "PreviouslyRegisteredController for practitioner" must {
    "return OK with the view when calling on page load" in {
      val request = addCSRFToken(FakeRequest(GET, routes.PreviouslyRegisteredController.onPageLoadPractitioner().url))
      val result = controller().onPageLoadPractitioner(request)

      status(result) mustBe OK
      contentAsString(result) mustBe view(formProvider(), AdministratorOrPractitioner.Practitioner)(request, messages).toString
    }

    "return a Bad Request and errors when invalid data is submitted" in {
      val postRequest = FakeRequest(POST, routes.PreviouslyRegisteredController.onSubmitPractitioner().url).withFormUrlEncodedBody("value" -> "invalid value")
      val boundForm = formProvider().bind(Map("value" -> "invalid value"))
      val result = controller().onSubmitPractitioner(postRequest)

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe view(boundForm, AdministratorOrPractitioner.Practitioner)(postRequest,messages).toString
    }

    "redirect to the correct next page when yes not logged in chosen (recovery URL)" in {
      val postRequest = FakeRequest(POST, routes.PreviouslyRegisteredController.onSubmitPractitioner().url).withFormUrlEncodedBody("value" ->
        PreviouslyRegistered.YesNotLoggedIn.toString)
      when(appConfig.recoverCredentialsPSPUrl).thenReturn(dummyUrl)
      val result = controller().onSubmitPractitioner(postRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe dummyUrl
    }

    "redirect to the correct next page when yes stopped in chosen" in {
      val postRequest = FakeRequest(POST, routes.PreviouslyRegisteredController.onSubmitPractitioner().url).withFormUrlEncodedBody("value" ->
        PreviouslyRegistered.YesStopped.toString)
      when(appConfig.registerSchemePractitionerUrl).thenReturn(dummyUrl)
      val result = controller().onSubmitPractitioner(postRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe dummyUrl
    }

    "redirect to the correct next page when no chosen" in {
      val postRequest = FakeRequest(POST, routes.PreviouslyRegisteredController.onSubmitPractitioner().url).withFormUrlEncodedBody("value" ->
        PreviouslyRegistered.No.toString)
      when(appConfig.registerSchemePractitionerUrl).thenReturn(dummyUrl)
      val result = controller().onSubmitPractitioner(postRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe dummyUrl
    }
  }

}




