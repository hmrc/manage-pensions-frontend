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
import controllers.actions.FakeAuthAction
import controllers.behaviours.ControllerWithQuestionPageBehaviours
import forms.CannotAccessPageAsAdministratorFormProvider
import models.AdministratorOrPractitioner
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.CSRFTokenHelper.addCSRFToken
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.Navigator
import views.html.cannotAccessPageAsAdministrator

class CannotAccessPageAsAdministratorControllerSpec
  extends ControllerWithQuestionPageBehaviours with ScalaFutures with MockitoSugar {
  val appConfig: FrontendAppConfig = mock[FrontendAppConfig]

  private val mockNavigator = mock[Navigator]

  private val view = injector.instanceOf[cannotAccessPageAsAdministrator]
  private val formProvider = new CannotAccessPageAsAdministratorFormProvider()
  val mockUserAnswersCacheConnector: UserAnswersCacheConnector = mock[UserAnswersCacheConnector]

  def controller: CannotAccessPageAsAdministratorController =
    new CannotAccessPageAsAdministratorController(appConfig, FakeAuthAction, messagesApi, formProvider, controllerComponents, view)

  "CannotAccessPageAsAdministratorController" must {

    "return OK with the view when calling on page load" in {
      val request = addCSRFToken(FakeRequest(GET, routes.CannotAccessPageAsAdministratorController.onPageLoad().url))
      val result = controller.onPageLoad(request)

      status(result) mustBe OK
      contentAsString(result) mustBe view(formProvider())(request, messages).toString
    }

    "redirect to the next page for a valid request" in {
      val postRequest = FakeRequest(POST, routes.CannotAccessPageAsAdministratorController.onSubmit().url).withFormUrlEncodedBody("value" ->
        AdministratorOrPractitioner.Administrator.toString)
      val result = controller.onSubmit(postRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe onwardRoute.url
    }
  }
}
