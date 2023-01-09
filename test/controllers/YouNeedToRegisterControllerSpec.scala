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

import controllers.actions._
import models.FeatureToggle.{Disabled, Enabled}
import models.FeatureToggleName.EnrolmentRecovery
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import services.FeatureToggleService
import views.html.{youNeedToRegister, youNeedToRegisterOld}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when

import scala.concurrent.Future


class YouNeedToRegisterControllerSpec extends ControllerSpecBase with MockitoSugar with BeforeAndAfterEach {

  val toggleService: FeatureToggleService = mock[FeatureToggleService]

  val view: youNeedToRegister = app.injector.instanceOf[youNeedToRegister]
  val viewOld: youNeedToRegisterOld = app.injector.instanceOf[youNeedToRegisterOld]


  def controller(dataRetrievalAction: DataRetrievalAction = getEmptyData): YouNeedToRegisterController =
    new YouNeedToRegisterController(messagesApi, controllerComponents, toggleService, view, viewOld)

  private def viewAsString() = view()(fakeRequest, messages).toString
  private def viewAsStringOld() = viewOld()(fakeRequest, messages).toString

  "YouNeedToRegister Controller" must {

    "return OK and the correct view for a GET when enrolment recovery toggle switched on" in {
      when(toggleService.get(any())(any(), any())).thenReturn(Future.successful(Enabled(EnrolmentRecovery)))
      val result = controller().onPageLoad(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString()
    }

    "return OK and the correct view for a GET when enrolment recovery toggle switched off" in {
      when(toggleService.get(any())(any(), any())).thenReturn(Future.successful(Disabled(EnrolmentRecovery)))
      val result = controller().onPageLoad(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe viewAsStringOld()
    }
  }
}




