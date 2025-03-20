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

package controllers.triagev2

import connectors.ManagePensionsCacheConnector
import controllers.ControllerSpecBase
import controllers.actions.{AuthAction, FakeAuthAction}
import forms.triagev2.WhatDoYouWantToDoFormProvider
import models.triagev2.WhatDoYouWantToDo.ManageExistingScheme
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.Json
import play.api.mvc.Call
import play.api.test.CSRFTokenHelper.addCSRFToken
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.annotations.TriageV2
import utils.{FakeNavigator, Navigator}
import views.html.triagev2.whatDoYouWantToDo

import scala.concurrent.Future

class WhatDoYouWantToDoControllerSpec extends ControllerSpecBase with ScalaFutures with MockitoSugar {

  private val onwardRoute = Call("GET", "/dummy")

  private val view = injector.instanceOf[whatDoYouWantToDo]
  private val formProvider = new WhatDoYouWantToDoFormProvider()
  private val mockAuthAction = mock[AuthAction]

  private val mockManagePensionsCacheConnector = mock[ManagePensionsCacheConnector]

  private val navigator = new FakeNavigator(onwardRoute)

  private def authAction: AuthAction =
    FakeAuthAction

  val controller: WhatDoYouWantToDoController = new WhatDoYouWantToDoController(
    messagesApi,
    navigator,
    authAction,
    formProvider,
    controllerComponents,
    view,
    mockManagePensionsCacheConnector
  )

  def beforeEach(): Unit = {
    reset(mockAuthAction)
    reset(mockManagePensionsCacheConnector)
  }

  val role = "administrator"

  private val getRoute: String = routes.WhatDoYouWantToDoController.onPageLoad(role).url
  private val postRoute: String = routes.WhatDoYouWantToDoController.onSubmit(role).url

  "WhatDoYouWantToDoController" must {

    "return OK with the view when calling on page load" in {
      when(mockManagePensionsCacheConnector.fetch(any())(any, any())).thenReturn(
        Future.successful(None))

      val request = addCSRFToken(FakeRequest(GET, getRoute))
      val result = controller.onPageLoad(role)(request)

      status(result) mustBe OK
      contentAsString(result) mustBe view(formProvider("administrator"), "administrator")(request, messages).toString
    }

    "return a Bad Request and errors when invalid data is submitted" in {
      when(mockManagePensionsCacheConnector.fetch(any())(any, any())).thenReturn(
        Future.successful(None))
      when(mockManagePensionsCacheConnector.save(any(), any(), any())(any, any(), any())).thenReturn(
        Future.successful(Json.obj("whatDoYouWantToDo" -> "opt1")))

      val request = addCSRFToken(FakeRequest(POST, postRoute).withFormUrlEncodedBody("value" -> "invalid value"))
      val result = controller.onSubmit(role)(request)
      val boundForm = formProvider(role).bind(Map("value" -> "invalid value"))

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe view(boundForm, "administrator")(request, messages).toString
    }

    "redirect to the next page for a valid request" in {
      when(mockManagePensionsCacheConnector.fetch(any())(any, any())).thenReturn(
        Future.successful(None))
      when(mockManagePensionsCacheConnector.save(any(), any(), any())(any, any(), any())).thenReturn(
        Future.successful(Json.obj("whatDoYouWantToDo" -> "opt1")))

      val request = addCSRFToken(FakeRequest(POST, postRoute).withFormUrlEncodedBody("value" -> "opt1"))
      val result = controller.onSubmit(role)(request)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe onwardRoute.url
    }
  }

}




