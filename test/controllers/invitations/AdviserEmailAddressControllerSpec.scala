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

package controllers.invitations

import controllers.ControllerSpecBase
import controllers.actions.{DataRequiredActionImpl, DataRetrievalAction, FakeAuthAction}
import forms.invitations.AdviserEmailFormProvider
import models.NormalMode
import play.api.test.Helpers._
import views.html.invitations.adviserEmailAddress

class AdviserEmailAddressControllerSpec extends ControllerSpecBase {

  val formProvider = new AdviserEmailFormProvider()
  val form = formProvider()
  def controller(
                data: DataRetrievalAction = getEmptyData
                ) =  new AdviserEmailAddressController(
    frontendAppConfig,
    messagesApi,
    FakeAuthAction(),
    data,
    new DataRequiredActionImpl,
    formProvider
  )

  val viewAsString = adviserEmailAddress(frontendAppConfig, form, NormalMode)(fakeRequest, messages).toString()

  "AdviserEmailAddressController " when {
    "on a GET" must {
      "return OK and the correct view" in {
        val result = controller().onPageLoad()(fakeRequest)
        status(result) mustBe OK
        contentAsString(result) mustBe viewAsString
      }
    }
  }

}
