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
import controllers.actions.{DataRequiredActionImpl, DataRetrievalAction, FakeAuthAction, FakeDataRetrievalAction}
import forms.invitations.DeclarationFormProvider
import play.api.test.Helpers._
import views.html.invitations.declaration
import utils._

class DeclarationControllerSpec extends ControllerSpecBase {

  val formProvider = new DeclarationFormProvider()
  val form = formProvider()
  val hasAdviser = true
  val isMasterTrust = false

  def controller(dataRetrievalAction: DataRetrievalAction = getEmptyData) = new DeclarationController(
    frontendAppConfig,
    messagesApi,
    formProvider,
    FakeAuthAction(),
    dataRetrievalAction,
    new DataRequiredActionImpl
  )

  val data = new FakeDataRetrievalAction(Some(UserAnswers().
    havePensionAdviser(hasAdviser).
    isMasterTrust(isMasterTrust).json
  ))

  def viewAsString = declaration(frontendAppConfig, hasAdviser, isMasterTrust, form)(fakeRequest, messages).toString

  "Declaration Controller" must {

    "Return OK and the correct view" in {
      val result = controller(data).onPageLoad()(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString
    }
  }

}
