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
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class PreviouslyRegisteredControllerSpec extends ControllerWithQuestionPageBehaviours with ScalaFutures with MockitoSugar with BeforeAndAfterEach {
  private val appConfig: FrontendAppConfig = mock[FrontendAppConfig]
  private val dummyUrl = "/url"
  private val tpssRecoveryURL = "/manage-pension-schemes/tpss-recovery"
  private val enrolmentPSA = Enrolment(
    key = "HMRC-PSA-ORG",
    identifiers = Seq(EnrolmentIdentifier(key = "PSAID", value = "A0000000")),
    state = "",
    delegatedAuthRule = None
  )
  private val enrolmentPSP = Enrolment(
    key = "HMRC-PP-ORG",
    identifiers = Seq(EnrolmentIdentifier(key = "PPID", value = "00000000")),
    state = "",
    delegatedAuthRule = None
  )
  private val enrolmentPODS = Enrolment(
    key = "HMRC-PODS-ORG",
    identifiers = Seq(EnrolmentIdentifier(key = "PSAID", value = "A2100005")),
    state = "",
    delegatedAuthRule = None
  )
  private val view = injector.instanceOf[previouslyRegistered]
  private val formProvider = new PreviouslyRegisteredFormProvider()

  private def fakeAuthConnector(stubbedRetrievalResult: Future[_]): AuthConnector = new AuthConnector {

    def authorise[A](predicate: Predicate, retrieval: Retrieval[A])
                    (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
      stubbedRetrievalResult.map(_.asInstanceOf[A])(ec)
  }

  override def beforeEach(): Unit = {
    reset(appConfig)
  }



  "PreviouslyRegisteredController for administrator" must {
    def controller(): PreviouslyRegisteredController =
      new PreviouslyRegisteredController(
        appConfig, fakeAuthConnector(Future.successful(Enrolments(Set(enrolmentPSA)))), messagesApi, formProvider, controllerComponents, view)

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

    "redirect to the correct next page when yes stopped in chosen" in {
      val postRequest = FakeRequest(POST, routes.PreviouslyRegisteredController.onSubmitAdministrator().url).withFormUrlEncodedBody("value" ->
        PreviouslyRegistered.PreviouslyRegisteredButStoppedBeingAdministrator.toString)
      when(appConfig.registerSchemeAdministratorUrl).thenReturn(dummyUrl)
      val result = controller().onSubmitAdministrator(postRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe dummyUrl
    }

    "redirect to the correct next page when no chosen" in {
      val postRequest = FakeRequest(POST, routes.PreviouslyRegisteredController.onSubmitAdministrator().url).withFormUrlEncodedBody("value" ->
        PreviouslyRegistered.NotPreviousRegistered.toString)
      when(appConfig.registerSchemeAdministratorUrl).thenReturn(dummyUrl)
      val result = controller().onSubmitAdministrator(postRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe dummyUrl
    }

    "redirect to the correct next page when yes not logged in chosen (recovery url) for psa" in {
      val postRequest = FakeRequest(POST, routes.PreviouslyRegisteredController.onSubmitAdministrator().url).withFormUrlEncodedBody("value" ->
        PreviouslyRegistered.PreviouslyRegisteredButNotLoggedIn.toString)
      when(appConfig.recoverCredentialsPSAUrl).thenReturn(tpssRecoveryURL)
      val result = controller().onSubmitAdministrator(postRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe tpssRecoveryURL
    }

    "redirect to the correct next page when yes not logged in chosen (recovery url)" in {
      val controller :PreviouslyRegisteredController =
        new PreviouslyRegisteredController(
          appConfig, fakeAuthConnector(Future.successful(Enrolments(Set(enrolmentPODS)))), messagesApi, formProvider, controllerComponents, view)
      val postRequest = FakeRequest(POST, routes.PreviouslyRegisteredController.onSubmitAdministrator().url).withFormUrlEncodedBody("value" ->
        PreviouslyRegistered.PreviouslyRegisteredButNotLoggedIn.toString)
      when(appConfig.recoverCredentialsPSAUrl).thenReturn(dummyUrl)
      val result = controller.onSubmitAdministrator(postRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe dummyUrl
    }
  }

  "PreviouslyRegisteredController for practitioner" must {

    def controller(): PreviouslyRegisteredController =
      new PreviouslyRegisteredController(
        appConfig, fakeAuthConnector(Future.successful(Enrolments(Set(enrolmentPSP)))), messagesApi, formProvider, controllerComponents, view)

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

    "redirect to the correct next page when yes stopped in chosen" in {
      val postRequest = FakeRequest(POST, routes.PreviouslyRegisteredController.onSubmitPractitioner().url).withFormUrlEncodedBody("value" ->
        PreviouslyRegistered.PreviouslyRegisteredButStoppedBeingAdministrator.toString)
      when(appConfig.registerSchemePractitionerUrl).thenReturn(dummyUrl)
      val result = controller().onSubmitPractitioner(postRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe dummyUrl
    }

    "redirect to the correct next page when no chosen" in {
      val postRequest = FakeRequest(POST, routes.PreviouslyRegisteredController.onSubmitPractitioner().url).withFormUrlEncodedBody("value" ->
        PreviouslyRegistered.NotPreviousRegistered.toString)
      when(appConfig.registerSchemePractitionerUrl).thenReturn(dummyUrl)
      val result = controller().onSubmitPractitioner(postRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe dummyUrl
    }


    "redirect to the correct next page when yes not logged in chosen (recovery URL) for psp" in {
      val postRequest = FakeRequest(POST, routes.PreviouslyRegisteredController.onSubmitPractitioner().url).withFormUrlEncodedBody("value" ->
        PreviouslyRegistered.PreviouslyRegisteredButNotLoggedIn.toString)
      when(appConfig.recoverCredentialsPSPUrl).thenReturn(tpssRecoveryURL)
      val result = controller().onSubmitPractitioner(postRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe tpssRecoveryURL
    }


    "redirect to the correct next page when yes not logged in chosen (recovery URL)" in {
      val controller : PreviouslyRegisteredController =
        new PreviouslyRegisteredController(
          appConfig, fakeAuthConnector(Future.successful(Enrolments(Set(enrolmentPODS)))), messagesApi, formProvider, controllerComponents, view)

      val postRequest = FakeRequest(POST, routes.PreviouslyRegisteredController.onSubmitPractitioner().url).withFormUrlEncodedBody("value" ->
        PreviouslyRegistered.PreviouslyRegisteredButNotLoggedIn.toString)
      when(appConfig.recoverCredentialsPSPUrl).thenReturn(dummyUrl)
      val result = controller.onSubmitPractitioner(postRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe dummyUrl
    }
  }

}




