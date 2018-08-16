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

package controllers

import controllers.actions.FakeAuthAction
import play.api.test.Helpers._

class LogoutControllerSpec extends ControllerSpecBase {

  def logoutController = new LogoutController(frontendAppConfig, messagesApi, FakeAuthAction)

  "Logout Controller" must {

    "redirect to feedback survey page for an Individual" in {
      val result = logoutController.onPageLoad(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(frontendAppConfig.serviceSignOut)
    }
  }
}
