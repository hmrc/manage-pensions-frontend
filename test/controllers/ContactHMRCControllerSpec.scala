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

package controllers

import play.api.test.Helpers._
import views.html.contactHMRC

class ContactHMRCControllerSpec extends ControllerSpecBase {
  val view: contactHMRC = app.injector.instanceOf[contactHMRC]
  "Contact HMRC Controller" must {
    "return 200 for a GET" in {
      val result = new ContactHMRCController(frontendAppConfig, messagesApi, controllerComponents, view).onPageLoad()(fakeRequest)
      status(result) mustBe OK
      contentAsString(result) mustBe view()(using fakeRequest, messages).toString
    }
  }
}
