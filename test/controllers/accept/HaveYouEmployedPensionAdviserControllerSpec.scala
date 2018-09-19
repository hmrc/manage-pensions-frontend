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
import play.api.test.Helpers._
import views.html.accept.haveYouEmployedPensionAdviser

class HaveYouEmployedPensionAdviserControllerSpec extends ControllerSpecBase {

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
  }
}
