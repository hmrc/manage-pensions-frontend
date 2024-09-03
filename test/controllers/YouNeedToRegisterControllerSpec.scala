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

import base.SpecBase
import models.FeatureToggle.{Disabled, Enabled}
import models.FeatureToggleName.EnrolmentRecovery
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import services.FeatureToggleService
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.HeaderCarrier
import views.html.{youNeedToRegister, youNeedToRegisterAsPsa, youNeedToRegisterAsPsp, youNeedToRegisterOld}

import scala.concurrent.{ExecutionContext, Future}


class YouNeedToRegisterControllerSpec extends ControllerSpecBase with MockitoSugar with BeforeAndAfterEach {

  import YouNeedToRegisterControllerSpec._

  val toggleService: FeatureToggleService = mock[FeatureToggleService]

  val registerAsPspView: youNeedToRegisterAsPsp = app.injector.instanceOf[youNeedToRegisterAsPsp]
  val registerAsPsaView: youNeedToRegisterAsPsa = app.injector.instanceOf[youNeedToRegisterAsPsa]
  val registerView: youNeedToRegister = app.injector.instanceOf[youNeedToRegister]
  val viewOld: youNeedToRegisterOld = app.injector.instanceOf[youNeedToRegisterOld]

  def controller(authConnector: AuthConnector): YouNeedToRegisterController =
    new YouNeedToRegisterController(
      authConnector,
      messagesApi,
      controllerComponents,
      toggleService,
      registerAsPspView,
      registerAsPsaView,
      registerView,
      viewOld
    )


  "YouNeedToRegister Controller" must {
    "return OK and the correct view for a GET when registered as PSA but not PSP" in {
      when(toggleService.get(any())(any(), any())).thenReturn(Future.successful(Enabled(EnrolmentRecovery)))
      val authConnector: AuthConnector = fakeAuthConnector(Future.successful(Enrolments(Set(enrolmentPSA))))

      val result = controller(authConnector).onPageLoad(fakeRequest)
      val viewAsString = registerAsPspView()(fakeRequest, messages).toString

      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString
    }

    "return OK and the correct view for a GET when registered as PSP but not PSA" in {
      when(toggleService.get(any())(any(), any())).thenReturn(Future.successful(Enabled(EnrolmentRecovery)))
      val authConnector: AuthConnector = fakeAuthConnector(Future.successful(Enrolments(Set(enrolmentPSP))))

      val result = controller(authConnector).onPageLoad(fakeRequest)
      val viewAsString = registerAsPsaView()(fakeRequest, messages).toString

      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString
    }

    "return OK and the correct view for a GET when registered as neither PSA or PSP enrolment recovery toggle switched on" in {
      when(toggleService.get(any())(any(), any())).thenReturn(Future.successful(Enabled(EnrolmentRecovery)))
      val authConnector: AuthConnector = fakeAuthConnector(Future.successful(Enrolments(Set(noEnrolments))))

      val result = controller(authConnector).onPageLoad(fakeRequest)
      val viewAsString = registerView()(fakeRequest, messages).toString

      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString
    }

    "return OK and the correct view for a GET when registered as neither PSA or PSP enrolment recovery toggle switched off" in {
      when(toggleService.get(any())(any(), any())).thenReturn(Future.successful(Disabled(EnrolmentRecovery)))
      val authConnector: AuthConnector = fakeAuthConnector(Future.successful(Enrolments(Set(noEnrolments))))

      val result = controller(authConnector).onPageLoad(fakeRequest)
      val viewAsStringOld = viewOld()(fakeRequest, messages).toString

      status(result) mustBe OK
      contentAsString(result) mustBe viewAsStringOld
    }
  }
}

object YouNeedToRegisterControllerSpec extends SpecBase with MockitoSugar {
  private val enrolmentPSP = Enrolment(
    key = "HMRC-PODSPP-ORG",
    identifiers = Seq(EnrolmentIdentifier(key = "PSPID", value = "20000000")),
    state = "",
    delegatedAuthRule = None
  )

  private val enrolmentPSA = Enrolment(
    key = "HMRC-PODS-ORG",
    identifiers = Seq(EnrolmentIdentifier(key = "PSAID", value = "A0000000")),
    state = "",
    delegatedAuthRule = None
  )

  private val noEnrolments = Enrolment(
    key = "",
    identifiers = Seq(EnrolmentIdentifier(key = "", value = "")),
    state = "",
    delegatedAuthRule = None
  )

  private def fakeAuthConnector(stubbedRetrievalResult: Future[_]): AuthConnector = new AuthConnector {

    def authorise[A](predicate: Predicate, retrieval: Retrieval[A])
                    (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
      stubbedRetrievalResult.map(_.asInstanceOf[A])(ec)
  }

}




