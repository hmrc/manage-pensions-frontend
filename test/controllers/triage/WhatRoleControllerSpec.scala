/*
 * Copyright 2022 HM Revenue & Customs
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
import forms.triage.WhatRoleFormProvider
import models.FeatureToggle.{Disabled, Enabled}
import models.FeatureToggleName.FinancialInformationAFT
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.mvc.Call
import play.api.test.CSRFTokenHelper.addCSRFToken
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, POST, contentAsString, defaultAwaitTimeout, redirectLocation, route, status, writeableOf_AnyContentAsEmpty, writeableOf_AnyContentAsFormUrlEncoded}
import services.FeatureToggleService
import utils.annotations.Triage
import utils.{FakeNavigator, Navigator}
import views.html.triage.whatRole

import scala.concurrent.Future

class WhatRoleControllerSpec extends ControllerSpecBase with ScalaFutures with MockitoSugar {
  private val mockFeatureToggleService = mock[FeatureToggleService]
  private val onwardRoute = Call("GET", "/dummy")
  private val application = applicationBuilder(Seq[GuiceableModule](bind[Navigator].
    qualifiedWith(classOf[Triage]).toInstance(new FakeNavigator(onwardRoute)), bind[FeatureToggleService].
    toInstance(mockFeatureToggleService))).build()

  private val view = injector.instanceOf[whatRole]
  private val formProvider = new WhatRoleFormProvider()


  private val toggle: Disabled = Disabled(FinancialInformationAFT)
  private val toggleEnabled: Enabled = Enabled(FinancialInformationAFT)
  "WhatRoleController" must {

    "return OK with the view when calling on page load" in {
      when(mockFeatureToggleService.getAftFeatureToggle(any())(any(), any()))
        .thenReturn(Future.successful(toggle))
      val request = addCSRFToken(FakeRequest(GET, routes.WhatRoleController.onPageLoad().url))
      val result = route(application, request).value

      status(result) mustBe OK
      contentAsString(result) mustBe view(formProvider())(request, messages).toString
    }
    "return OK with the view when calling on page load with toggle Enabled" in {
      when(mockFeatureToggleService.getAftFeatureToggle(any())(any(), any()))
        .thenReturn(Future.successful(toggleEnabled))
      val request = addCSRFToken(FakeRequest(GET, routes.WhatRoleController.onPageLoad().url))
      val result = route(application, request).value

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe controllers.triagev2.routes.WhatRoleControllerV2.onPageLoad().url
    }

    "redirect to the next page for a valid request" in {
      when(mockFeatureToggleService.getAftFeatureToggle(any())(any(), any()))
        .thenReturn(Future.successful(toggle))
      val postRequest = FakeRequest(POST, routes.WhatRoleController.onSubmit().url).withFormUrlEncodedBody("value" -> "PSA")
      val result = route(application, postRequest).value

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe onwardRoute.url
    }
  }

  "return a Bad Request and errors when invalid data is submitted" in {
    when(mockFeatureToggleService.getAftFeatureToggle(any())(any(), any()))
      .thenReturn(Future.successful(toggle))
    val postRequest = FakeRequest(POST, routes.WhatRoleController.onSubmit().url).withFormUrlEncodedBody("value" -> "invalid value")
    val result = route(application, postRequest).value
    val boundForm = formProvider().bind(Map("value" -> "invalid value"))
    status(result) mustBe BAD_REQUEST
    contentAsString(result) mustBe view(boundForm)(postRequest, messages).toString
  }
}
