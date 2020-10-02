/*
 * Copyright 2020 HM Revenue & Customs
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

package controllers.invitations.psp

import base.JsonFileReader
import controllers.ControllerSpecBase
import controllers.actions.DataRequiredActionImpl
import controllers.actions.DataRetrievalAction
import controllers.actions.FakeAuthAction
import forms.invitations.psp.DeclarationFormProvider
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.Configuration
import play.api.data.Form
import play.api.mvc.Call
import play.api.test.Helpers._
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import views.html.invitations.psp.declaration

class DeclarationControllerSpec extends ControllerSpecBase with MockitoSugar with BeforeAndAfterEach with JsonFileReader {

  def onwardRoute: Call = controllers.invitations.psp.routes.ConfirmationController.onPageLoad()

  val formProvider = new DeclarationFormProvider()
  val form: Form[Boolean] = formProvider()

  val config: Configuration = injector.instanceOf[Configuration]
  private val view = injector.instanceOf[declaration]
  private def sessionExpired: String = controllers.routes.SessionExpiredController.onPageLoad().url

  def controller(dataRetrievalAction: DataRetrievalAction = getEmptyData) = new DeclarationController(
    frontendAppConfig,
    messagesApi,
    formProvider,
    FakeAuthAction(),
    dataRetrievalAction,
    new DataRequiredActionImpl,
    stubMessagesControllerComponents(),
    view
  )

  private def viewAsString(form: Form[_] = form) = view(form)(fakeRequest, messages).toString

  "Declaration Controller" when {

    "on a GET" must {

      "return OK and the correct view" in {
        val result = controller().onPageLoad()(fakeRequest)

        status(result) mustBe OK
        contentAsString(result) mustBe viewAsString()
      }

      "redirect to Session Expired page if there is no cached data" in {
        val result = controller(dontGetAnyData).onPageLoad()(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe sessionExpired
      }
    }

    "on a POST" must {

      "invite psp and redirect to next page when valid data is submitted" in {
        val result = controller().onSubmit()(fakeRequest.withFormUrlEncodedBody("agree" -> "agreed"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe onwardRoute.url
        }

      "return Bad Request if invalid data is submitted" in {
        val formWithErrors = form.withError("agree", messages("messages__error__psp_declaration__required"))
        val result = controller().onSubmit()(fakeRequest)
        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe viewAsString(formWithErrors)
      }

      "redirect to Session Expired page if there is no cached data" in {
        val result = controller(dontGetAnyData).onSubmit()(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe sessionExpired
      }
    }
  }
}
