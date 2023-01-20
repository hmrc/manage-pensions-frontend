/*
 * Copyright 2023 HM Revenue & Customs
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

import config.FrontendAppConfig
import connectors.admin.MinimalConnector
import connectors.{EmailConnector, EmailNotSent}
import controllers.actions.FakeAuthAction
import controllers.behaviours.ControllerWithQuestionPageBehaviours
import forms.UrBannerFormProvider
import models.{IndividualDetails, MinimalPSAPSP, URBanner}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.data.Form
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.banner

import scala.concurrent.Future

class BannerControllerSpec extends ControllerWithQuestionPageBehaviours with ScalaFutures with MockitoSugar {

  val appConfig: FrontendAppConfig = mock[FrontendAppConfig]

  private val view = injector.instanceOf[banner]
  private val formProvider = new UrBannerFormProvider()
  val mockEmailConnector: EmailConnector = mock[EmailConnector]
  val mockMinimalConnector: MinimalConnector = mock[MinimalConnector]

  def controller: BannerController =
    new BannerController(
      appConfig,
      formProvider,
      mockMinimalConnector,
      mockEmailConnector,
      messagesApi,
      FakeAuthAction,
      controllerComponents,
      view)

  private val minDetails = MinimalPSAPSP("email", false, None, Some(IndividualDetails("Nigel", None, "Smith")), false, false)

  "BannerController" must {

    "return OK with the view when calling on page load" in {
      when(mockMinimalConnector.getMinimalPsaDetails(any())(any(), any())).thenReturn(Future.successful(minDetails))
      val form = formProvider.apply().fill(URBanner("Nigel Smith", "email"))
      val request = FakeRequest(GET, routes.BannerController.onPageLoad.url)
      val result = controller.onPageLoad(request)
      print(s"\n\n here \n\n ${request.body} \n\n")
      status(result) mustBe OK
      contentAsString(result) mustBe view(form)(request, messages).toString
    }

    "return a Bad Request and errors when invalid data is submitted" in {
      when(mockMinimalConnector.getMinimalPsaDetails(any())(any(), any())).thenReturn(Future.successful(minDetails))
      when(mockEmailConnector.sendEmail(any())(any(), any())).thenReturn(Future.successful(EmailNotSent))
      val postRequest =
        FakeRequest(POST, routes.BannerController.onSubmit.url).withFormUrlEncodedBody("indOrgName" -> "invalid value")
      val boundForm =
        formProvider().bind(Map("indOrgName" -> "invalid value"))
      val result =
        controller.onSubmit(postRequest)

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe view(boundForm)(postRequest,messages).toString
    }
  }
}




