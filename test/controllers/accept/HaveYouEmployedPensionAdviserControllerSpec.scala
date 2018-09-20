/*
 * Copyright 2018 HM Revenue & Customs
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

package controllers.accept

import controllers.ControllerSpecBase
import controllers.actions.FakeAuthAction
import forms.accept.HaveYouEmployedPensionAdviserFormProvider
import models.NormalMode
import play.api.mvc.Call
import play.api.test.Helpers._
import views.html.accept.haveYouEmployedPensionAdviser

class HaveYouEmployedPensionAdviserControllerSpec extends ControllerSpecBase {

  def onwardRoute = Call("GET", "/foo")

  "HaveYouEmployedPensionAdviserSpec" must {

    val controller = new HaveYouEmployedPensionAdviserController(
      frontendAppConfig,
      FakeAuthAction(),
      messagesApi,
      new HaveYouEmployedPensionAdviserFormProvider()
    )

    val form = new HaveYouEmployedPensionAdviserFormProvider()()

    val viewAsString = haveYouEmployedPensionAdviser(frontendAppConfig, form)(fakeRequest, messages).toString

    "Return 200 and view" in {

      val result = controller.onPageLoad()(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString

    }

    "redirect to the next page when valid data is submitted" in {
      val postRequest = fakeRequest.withFormUrlEncodedBody(("employed", "true"))

      val result = controller.onSubmit()(postRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(onwardRoute.url)
    }
  }
}
