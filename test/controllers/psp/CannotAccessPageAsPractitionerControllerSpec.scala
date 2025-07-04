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

package controllers.psp

import config.FrontendAppConfig
import connectors.SessionDataCacheConnector
import controllers.actions.FakeAuthAction
import controllers.behaviours.ControllerWithQuestionPageBehaviours
import controllers.psp.routes.{CannotAccessPageAsPractitionerController, PspDashboardController}
import forms.psp.CannotAccessPageAsPractitionerFormProvider
import models.AdministratorOrPractitioner
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsNull, JsValue, Json}
import play.api.test.CSRFTokenHelper.addCSRFToken
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.UserAnswers
import views.html.psp.cannotAccessPageAsPractitioner

import scala.concurrent.Future

class CannotAccessPageAsPractitionerControllerSpec
  extends ControllerWithQuestionPageBehaviours with ScalaFutures with MockitoSugar with BeforeAndAfterEach {
  val appConfig: FrontendAppConfig = mock[FrontendAppConfig]

  private val view = injector.instanceOf[cannotAccessPageAsPractitioner]
  private val formProvider = new CannotAccessPageAsPractitionerFormProvider()
  private val mockSessionDataCacheConnector: SessionDataCacheConnector = mock[SessionDataCacheConnector]

  def controller: CannotAccessPageAsPractitionerController =
    new CannotAccessPageAsPractitionerController(appConfig, FakeAuthAction, messagesApi,
      mockSessionDataCacheConnector, formProvider, controllerComponents, view)

  override def beforeEach(): Unit = {
    reset(mockSessionDataCacheConnector)
    when(mockSessionDataCacheConnector.save(any(), any(), any())(using any(), any(), any()))
      .thenReturn(Future.successful(JsNull))
    super.beforeEach()
  }

  private val continueUrl = "/test"

  private val uaWithContinueUrl = UserAnswers(Json.obj("continueURL" -> continueUrl))

  "CannotAccessPageAsPractitionerController" must {

    "return OK with the view when calling on page load with continue parameter" in {
      val request = addCSRFToken(FakeRequest(GET, s"${CannotAccessPageAsPractitionerController.onPageLoad().url}?continue=/test" ))
      val result = controller.onPageLoad(request)

      status(result) mustBe OK
      contentAsString(result) mustBe view(formProvider())(using request, messages).toString
    }

    "redirect when calling on page load with no continue parameter" in {
      val request = addCSRFToken(FakeRequest(GET, CannotAccessPageAsPractitionerController.onPageLoad().url))
      val result = controller.onPageLoad(request)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(PspDashboardController.onPageLoad().url)
    }

    "redirect to the practitioner dashboard page for a valid request where practitioner chosen" in {
      when(mockSessionDataCacheConnector.fetch(any())(using any(), any()))
        .thenReturn(Future.successful(Some(uaWithContinueUrl.json)))
      when(mockSessionDataCacheConnector.upsert(any(), any())(using any(), any()))
        .thenReturn(Future.successful(JsNull))

      val postRequest = FakeRequest(POST, CannotAccessPageAsPractitionerController.onSubmit().url).withFormUrlEncodedBody(
        "value" -> AdministratorOrPractitioner.Practitioner.toString
      )
      val result = controller.onSubmit(postRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(PspDashboardController.onPageLoad().url)
    }

    "redirect to the page in parameter for a valid request where administrator chosen and update the role in mongo" in {
      when(mockSessionDataCacheConnector.fetch(any())(using any(), any()))
        .thenReturn(Future.successful(Some(uaWithContinueUrl.json)))
      when(mockSessionDataCacheConnector.upsert(any(), any())(using any(), any()))
        .thenReturn(Future.successful(JsNull))
      val postRequest = FakeRequest(POST, CannotAccessPageAsPractitionerController.onSubmit().url).withFormUrlEncodedBody(
        "value" -> AdministratorOrPractitioner.Administrator.toString
      )
      val result = controller.onSubmit(postRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(continueUrl)
      val jsonCaptor = ArgumentCaptor.forClass(classOf[JsValue])
      val expectedSessionCacheJson = Json.obj{"administratorOrPractitioner" -> "administrator"}
      verify(mockSessionDataCacheConnector, times(1)).upsert(any(), jsonCaptor.capture())(using any(), any())
      jsonCaptor.getValue mustBe expectedSessionCacheJson
    }
  }
}
