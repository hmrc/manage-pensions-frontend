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

import connectors.FakeDataCacheConnector
import controllers.ControllerSpecBase
import controllers.actions.{FakeAuthAction, FakeDataRetrievalAction}
import forms.accept.HaveYouEmployedPensionAdviserFormProvider
import models.NormalMode
import play.api.libs.json.Json
import play.api.mvc.Call
import play.api.test.Helpers._
import utils.FakeNavigator
import views.html.accept.haveYouEmployedPensionAdviser

class HaveYouEmployedPensionAdviserControllerSpec extends ControllerSpecBase {

  def onwardRoute = Call("GET", "/foo")

  val formProvider = new HaveYouEmployedPensionAdviserFormProvider()
  val form = formProvider()

  "HaveYouEmployedPensionAdviserSpec" must {

    val controller = new HaveYouEmployedPensionAdviserController(
      frontendAppConfig,
      FakeAuthAction(),
      messagesApi,
      FakeNavigator,
      formProvider,
      FakeDataCacheConnector,
      new FakeDataRetrievalAction(Some(Json.obj()))
    )

    val form = new HaveYouEmployedPensionAdviserFormProvider()()

    val viewAsString = haveYouEmployedPensionAdviser(frontendAppConfig, form, NormalMode)(fakeRequest, messages).toString

    "Return 200 and view" in {

      val result = controller.onPageLoad(NormalMode)(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString

    }

    "redirect to the next page when valid data is submitted" in {
      val postRequest = fakeRequest.withFormUrlEncodedBody(("employed", "true"))

      val result = controller.onSubmit(NormalMode)(postRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(onwardRoute.url)
    }
  }
}
