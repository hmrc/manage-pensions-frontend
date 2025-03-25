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
import forms.triagev2.WhichServiceYouWantToViewFormProvider
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.mvc.Call
import play.api.test.CSRFTokenHelper.addCSRFToken
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.FakeNavigator
import views.html.triagev2.whichServiceYouWantToView

import scala.concurrent.Future

class WhichServiceYouWantToViewControllerSpec extends ControllerSpecBase with ScalaFutures with MockitoSugar {

  private val onwardRoute = Call("GET", "/dummy")

  private val navigator = new FakeNavigator(onwardRoute)
  private val mockAuthAction = mock[AuthAction]
  private val mockManagePensionsCacheConnector = mock[ManagePensionsCacheConnector]

  private val view = injector.instanceOf[whichServiceYouWantToView]
  private val formProvider = new WhichServiceYouWantToViewFormProvider()

  private def authAction: AuthAction = FakeAuthAction

  val controller: WhichServiceYouWantToViewController = new WhichServiceYouWantToViewController(
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

  private val role = "administrator"

  private val form = formProvider(role)

  private val getRoute: String = routes.WhichServiceYouWantToViewController.onPageLoad(role).url
  private val postRoute: String = routes.WhichServiceYouWantToViewController.onSubmit(role).url

  "WhichServiceYouWantToViewController" must {

    "return OK with the view when calling on page load" in {
      when(mockManagePensionsCacheConnector.fetch(any())(any, any())).thenReturn(
        Future.successful(None))

      val request = addCSRFToken(FakeRequest(GET, getRoute))
      val result = controller.onPageLoad(role)(request)

      status(result) mustBe OK
      contentAsString(result) mustBe view(form, role)(request, messages).toString
    }

    "return a Bad Request and errors when invalid data is submitted" in {
      when(mockManagePensionsCacheConnector.fetch(any())(any, any())).thenReturn(
        Future.successful(None))
      when(mockManagePensionsCacheConnector.save(any(), any(), any())(any, any(), any())).thenReturn(
        Future.successful(Json.obj("whichServiceYouWantToView" -> "opt1")))

      val request = addCSRFToken(FakeRequest(POST, postRoute).withFormUrlEncodedBody("value" -> "invalid value"))
      val result = controller.onSubmit(role)(request)
      val boundForm = formProvider("administrator").bind(Map("value" -> "invalid value"))

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe view(boundForm, "administrator")(request, messages).toString
    }

    "redirect to the next page for a valid request" in {
      when(mockManagePensionsCacheConnector.fetch(any())(any, any())).thenReturn(
        Future.successful(None))
      when(mockManagePensionsCacheConnector.save(any(), any(), any())(any, any(), any())).thenReturn(
        Future.successful(Json.obj("whichServiceYouWantToView" -> "opt1")))

      val request = addCSRFToken(FakeRequest(POST, postRoute).withFormUrlEncodedBody("value" -> "opt1"))
      val result = controller.onSubmit(role)(request)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe onwardRoute.url
    }
  }

}




