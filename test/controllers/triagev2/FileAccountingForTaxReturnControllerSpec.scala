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

package controllers.triagev2

import controllers.ControllerSpecBase
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.CSRFTokenHelper.addCSRFToken
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.triagev2.fileAccountingForTaxReturn

class FileAccountingForTaxReturnControllerSpec extends ControllerSpecBase with ScalaFutures with MockitoSugar {

  private val application = applicationBuilder().build()

  private val view = injector.instanceOf[fileAccountingForTaxReturn]
  private val psaOverviewLink = s"${frontendAppConfig.loginUrl}?continue=${frontendAppConfig.psaOverviewUrl}"
  private val pspOverviewLink =  s"${frontendAppConfig.loginUrl}?continue=${frontendAppConfig.pspDashboardUrl}"

  "FileAccountingForTaxReturnController" must {

    "return OK with the view when calling on page load for PSA" in {
      val role = "administrator"
      val request = addCSRFToken(FakeRequest(GET, routes.FileAccountingForTaxReturnController.onPageLoad(role).url))
      val result = route(application, request).value

      status(result) mustBe OK
      contentAsString(result) mustBe
        view(psaOverviewLink, frontendAppConfig.tpssWelcomeUrl)(request, messages).toString
    }

    "return OK with the view when calling on page load for PSP" in {
      val role = "practitioner"
      val request = addCSRFToken(FakeRequest(GET, routes.FileAccountingForTaxReturnController.onPageLoad(role).url))
      val result = route(application, request).value

      status(result) mustBe OK
      contentAsString(result) mustBe
        view(pspOverviewLink, frontendAppConfig.tpssWelcomeUrl)(request, messages).toString
    }
  }
}




