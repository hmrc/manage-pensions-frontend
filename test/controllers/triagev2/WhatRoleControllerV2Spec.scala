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
import forms.triagev2.WhatRoleFormProviderV2
import models.triagev2.WhatRole
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.json.Json
import play.api.mvc.Call
import play.api.test.CSRFTokenHelper.addCSRFToken
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, POST, contentAsString, defaultAwaitTimeout, redirectLocation, status}
import utils.FakeNavigator
import views.html.triagev2.whatRole

import scala.concurrent.Future

class WhatRoleControllerV2Spec extends ControllerSpecBase with ScalaFutures with MockitoSugar {

  private val onwardRoute = Call("GET", "/dummy")
  private val navigator = new FakeNavigator(onwardRoute)
  private val mockAuthAction = mock[AuthAction]
  private val mockManagePensionsCacheConnector = mock[ManagePensionsCacheConnector]

  private val view = injector.instanceOf[whatRole]
  private val formProvider = new WhatRoleFormProviderV2
  private val form = formProvider()

  private def authAction: AuthAction =
    FakeAuthAction

  val controller: WhatRoleControllerV2 = new WhatRoleControllerV2(
    messagesApi,
    navigator,
    authAction,
    formProvider,
    controllerComponents,
    mockManagePensionsCacheConnector,
    view
  )
  def beforeEach(): Unit = {
    reset(mockAuthAction)
    reset(mockManagePensionsCacheConnector)
  }

  private val getRoute: String = routes.WhatRoleControllerV2.onPageLoad.url
  private val postRoute: String = routes.WhatRoleControllerV2.onSubmit.url

  "WhatRoleControllerV2" must {

    "return OK with the view when calling on page load" in {

      when(mockManagePensionsCacheConnector.fetch(any())(using any, any())).thenReturn(
        Future.successful(None))

      val request = addCSRFToken(FakeRequest(GET, getRoute))
      val result = controller.onPageLoad(request)

      status(result) mustBe OK
      contentAsString(result) mustBe view(form)(using request, messages).toString
    }
    "return OK with the view when calling on page load and question previously answered" in {

      when(mockManagePensionsCacheConnector.fetch(any())(using any, any())).thenReturn(
        Future.successful(Some(Json.obj("whatRole" -> "administrator"))))

      val request = addCSRFToken(FakeRequest(GET, getRoute))
      val result = controller.onPageLoad(request)

      status(result) mustBe OK
      contentAsString(result) mustBe view(form.fill(WhatRole.PSA))(using request, messages).toString
    }

    "redirect to the next page for a valid request" in {
      when(mockManagePensionsCacheConnector.fetch(any())(using any, any())).thenReturn(
        Future.successful(None))
      when(mockManagePensionsCacheConnector.save(any(), any(), any())(using any, any(), any())).thenReturn(
        Future.successful(Json.obj("whatRole" -> "administrator")))

      val request = addCSRFToken(FakeRequest(POST, postRoute).withFormUrlEncodedBody("value" -> "administrator"))
      val result = controller.onSubmit(request)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe onwardRoute.url
    }
  }

  "return a Bad Request and errors when invalid data is submitted" in {
    when(mockManagePensionsCacheConnector.fetch(any())(using any, any())).thenReturn(
      Future.successful(None))
    when(mockManagePensionsCacheConnector.save(any(), any(), any())(using any, any(), any())).thenReturn(
      Future.successful(Json.obj("whatRole" -> "administrator")))

    val request = addCSRFToken(FakeRequest(POST, postRoute).withFormUrlEncodedBody("value" -> "invalid value"))
    val result = controller.onSubmit(request)

    val boundForm = formProvider().bind(Map("value" -> "invalid value"))
    status(result) mustBe BAD_REQUEST
    contentAsString(result) mustBe view(boundForm)(using request, messages).toString
  }
}
