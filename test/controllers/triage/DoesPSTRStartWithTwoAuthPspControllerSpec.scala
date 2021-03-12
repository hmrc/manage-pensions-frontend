/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers.triage

import controllers.ControllerSpecBase
import forms.triage.DoesPSTRStartWithTwoFormProvider
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.mvc.Call
import play.api.test.CSRFTokenHelper.addCSRFToken
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.{FakeNavigator, Navigator}
import utils.annotations.Triage
import views.html.triage.doesPSTRStartWithTwo

class DoesPSTRStartWithTwoAuthPspControllerSpec extends ControllerSpecBase with ScalaFutures with MockitoSugar {

  private val onwardRoute = Call("GET", "/dummy")
  private val application = applicationBuilder(Seq[GuiceableModule](bind[Navigator].
    qualifiedWith(classOf[Triage]).toInstance(new FakeNavigator(onwardRoute)))).build()
  private def postCall: Call = controllers.triage.routes.DoesPSTRStartWithTwoAuthPspController.onSubmit()
  private val view = injector.instanceOf[doesPSTRStartWithTwo]
  private val formProvider = new DoesPSTRStartWithTwoFormProvider()
  private val hint: Option[String] = Some(messages("messages__doesPSTRStartWithTwo_authPsp__hint"))

  "DoesPSTRStartWithTwoAuthPspController" must {

    "return OK with the view when calling on page load" in {
      val request = addCSRFToken(FakeRequest(GET, routes.DoesPSTRStartWithTwoAuthPspController.onPageLoad().url))
      val result = route(application, request).value

      status(result) mustBe OK
      contentAsString(result) mustBe view(formProvider(), postCall, hint)(request, messages).toString
    }

    "return a Bad Request and errors when invalid data is submitted" in {
      val postRequest = FakeRequest(POST, routes.DoesPSTRStartWithTwoAuthPspController.onSubmit().url).withFormUrlEncodedBody("value" -> "invalid value")
      val boundForm = formProvider().bind(Map("value" -> "invalid value"))
      val result = route(application, postRequest).value

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe view(boundForm, postCall, hint)(postRequest, messages).toString
    }

    "redirect to the next page for a valid request" in {
      val postRequest = FakeRequest(POST, routes.DoesPSTRStartWithTwoAuthPspController.onSubmit().url).withFormUrlEncodedBody("value" -> "opt1")
      val result = route(application, postRequest).value

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe onwardRoute.url
    }
  }

}




