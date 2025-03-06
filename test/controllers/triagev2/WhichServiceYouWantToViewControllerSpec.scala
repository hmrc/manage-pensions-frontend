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

import connectors.UserAnswersCacheConnector
import controllers.ControllerSpecBase
import forms.triagev2.WhichServiceYouWantToViewFormProvider
import models.triagev2.WhichServiceYouWantToView.ManagingPensionSchemes
import org.scalatest.concurrent.ScalaFutures
import controllers.actions.{AuthAction, DataRequiredAction, DataRequiredActionImpl, DataRetrievalAction, FakeAuthAction, FakeDataRetrievalAction}
import models.triagev2.WhatRole
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.{Format, Json}
import play.api.mvc.Call
import play.api.test.CSRFTokenHelper.addCSRFToken
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.annotations.TriageV2
import utils.{FakeNavigator, Navigator, UserAnswers}
import views.html.triagev2.whichServiceYouWantToView

import scala.concurrent.Future

class WhichServiceYouWantToViewControllerSpec extends ControllerSpecBase with ScalaFutures with MockitoSugar {

  private val onwardRoute = Call("GET", "/dummy")
  private val fakeUserAnswersCacheConnector = mock[UserAnswersCacheConnector]

  private val application = applicationBuilder(Seq[GuiceableModule](
    bind[Navigator].qualifiedWith(classOf[TriageV2]).toInstance(new FakeNavigator(onwardRoute)),
    bind[UserAnswersCacheConnector].toInstance(fakeUserAnswersCacheConnector),
    bind[AuthAction].toInstance(FakeAuthAction),
    bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(Some(UserAnswers().json))),
    bind[DataRequiredAction].to[DataRequiredActionImpl]
  )).build()

  private val view = injector.instanceOf[whichServiceYouWantToView]
  private val formProvider = new WhichServiceYouWantToViewFormProvider()

  def beforeEach(): Unit = {
    beforeEach()
    reset(fakeUserAnswersCacheConnector)
  }

  "WhichServiceYouWantToViewController" must {

    "return OK with the view when calling on page load" in {
      val request = addCSRFToken(FakeRequest(GET, routes.WhichServiceYouWantToViewController.onPageLoad("administrator").url))
      val result = route(application, request).value

      status(result) mustBe OK
      contentAsString(result) mustBe view(formProvider("administrator"), "administrator")(request, messages).toString
    }

    "return a Bad Request and errors when invalid data is submitted" in {
      val postRequest = addCSRFToken(
        FakeRequest(POST, routes.WhichServiceYouWantToViewController.onSubmit("administrator").url)
          .withFormUrlEncodedBody("value" -> "invalid value")
      )
      val boundForm = formProvider("administrator").bind(Map("value" -> "invalid value"))
      val result = route(application, postRequest).value

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe view(boundForm, "administrator")(postRequest, messages).toString
    }

    "redirect to the next page for a valid request" in {
      when(fakeUserAnswersCacheConnector.save(any[String], any(), any[WhichServiceYouWantToViewController])(any[Format[WhichServiceYouWantToViewController]], any(), any()))
        .thenReturn(Future.successful(Json.obj()))

      val postRequest = FakeRequest(POST, routes.WhichServiceYouWantToViewController.onSubmit("administrator").url)
        .withFormUrlEncodedBody("value" -> ManagingPensionSchemes.toString)
      val result = route(application, postRequest).value

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe onwardRoute.url
    }
  }

}




