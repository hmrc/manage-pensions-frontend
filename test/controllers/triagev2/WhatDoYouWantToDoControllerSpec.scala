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

import controllers.ControllerSpecBase
import forms.triagev2.WhatDoYouWantToDoFormProvider
import models.triagev2.WhatDoYouWantToDo.ManageExistingScheme
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.mvc.Call
import play.api.test.CSRFTokenHelper.addCSRFToken
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.annotations.TriageV2
import utils.{FakeNavigator, Navigator}
import views.html.triagev2.whatDoYouWantToDo

class WhatDoYouWantToDoControllerSpec extends ControllerSpecBase with ScalaFutures with MockitoSugar {

  private val onwardRoute = Call("GET", "/dummy")
  private val application = applicationBuilder(Seq[GuiceableModule](bind[Navigator].
    qualifiedWith(classOf[TriageV2]).toInstance(new FakeNavigator(onwardRoute)))).build()

  private val view = injector.instanceOf[whatDoYouWantToDo]
  private val formProvider = new WhatDoYouWantToDoFormProvider()

  "WhatDoYouWantToDoController" must {

    "return OK with the view when calling on page load" in {
      val request = addCSRFToken(FakeRequest(GET, routes.WhatDoYouWantToDoController.onPageLoad("administrator").url))
      val result = route(application, request).value

      status(result) mustBe OK
      contentAsString(result) mustBe view(formProvider("administrator"), "administrator")(request, messages).toString
    }

    "return a Bad Request and errors when invalid data is submitted" in {
      val postRequest = FakeRequest(POST, routes.WhatDoYouWantToDoController.onSubmit("administrator").url).withFormUrlEncodedBody("value" -> "invalid value")
      val boundForm = formProvider("administrator").bind(Map("value" -> "invalid value"))
      val result = route(application, postRequest).value

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe view(boundForm, "administrator")(postRequest, messages).toString

    }

    "redirect to the next page for a valid request" in {
      val postRequest = FakeRequest(POST, routes.WhatDoYouWantToDoController.onSubmit("administrator").url)
        .withFormUrlEncodedBody("value" -> ManageExistingScheme.toString)
      val result = route(application, postRequest).value

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe onwardRoute.url
    }
  }

}




