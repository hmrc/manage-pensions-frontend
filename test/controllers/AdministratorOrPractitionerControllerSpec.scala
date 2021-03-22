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

package controllers

import config.FrontendAppConfig
import connectors.UserAnswersCacheConnector
import controllers.actions.{DataRetrievalAction, FakeAuthAction}
import controllers.behaviours.ControllerWithQuestionPageBehaviours
import forms.AdministratorOrPractitionerFormProvider
import models.AdministratorOrPractitioner
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import play.api.libs.json.Json
import play.api.test.CSRFTokenHelper.addCSRFToken
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.Navigator
import views.html.administratorOrPractitioner

import scala.concurrent.Future

class AdministratorOrPractitionerControllerSpec extends ControllerWithQuestionPageBehaviours with ScalaFutures with MockitoSugar {
  val appConfig: FrontendAppConfig = mock[FrontendAppConfig]

  private val mockNavigator = mock[Navigator]

  private val view = injector.instanceOf[administratorOrPractitioner]
  private val formProvider = new AdministratorOrPractitionerFormProvider()
  val mockUserAnswersCacheConnector: UserAnswersCacheConnector = mock[UserAnswersCacheConnector]

  def controller(dataRetrievalAction: DataRetrievalAction = dontGetAnyData): AdministratorOrPractitionerController =
    new AdministratorOrPractitionerController(
      appConfig, FakeAuthAction, messagesApi, mockNavigator, formProvider,
      mockUserAnswersCacheConnector, dataRetrievalAction, requiredDataAction, controllerComponents, view)

  "AdministratorOrPractitionerController" must {

    "return OK with the view when calling on page load" in {
      val request = addCSRFToken(FakeRequest(GET, routes.AdministratorOrPractitionerController.onPageLoad().url))
      val result = controller().onPageLoad(request)

      status(result) mustBe OK
      contentAsString(result) mustBe view(formProvider())(request, messages).toString
    }

    "return a Bad Request and errors when invalid data is submitted" in {
      val postRequest = FakeRequest(POST, routes.AdministratorOrPractitionerController.onSubmit().url).withFormUrlEncodedBody("value" -> "invalid value")
      val boundForm = formProvider().bind(Map("value" -> "invalid value"))
      val result = controller().onSubmit(postRequest)

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe view(boundForm)(postRequest,messages).toString
    }

    "redirect to the next page for a valid request" in {
      when(mockUserAnswersCacheConnector.save(any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(Json.obj()))
      when(mockNavigator.nextPage(any(), any(), any())).thenReturn(onwardRoute)
      val postRequest = FakeRequest(POST, routes.AdministratorOrPractitionerController.onSubmit().url).withFormUrlEncodedBody("value" ->
        AdministratorOrPractitioner.Administrator.toString)
      val result = controller().onSubmit(postRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe onwardRoute.url
    }
  }

}




