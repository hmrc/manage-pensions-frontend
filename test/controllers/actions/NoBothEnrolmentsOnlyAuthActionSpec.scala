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

package controllers.actions

import base.SpecBase
import controllers.actions.AuthActionSpec._
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.{NoActiveSession, _}
import uk.gov.hmrc.auth.core.retrieve.~

import java.net.URLEncoder
import scala.concurrent.Future
class NoBothEnrolmentsOnlyAuthActionSpec
  extends SpecBase {

  private def action(enrolments: Set[Enrolment]): NoBothEnrolmentsOnlyAuthAction = action(NoBothEnrolmentsOnlyAuthActionSpec.authRetrievals(enrolments))

  private def action(stubbedRetrievalResult: Future[?]): NoBothEnrolmentsOnlyAuthAction = new NoBothEnrolmentsOnlyAuthAction(
    authConnector = fakeAuthConnector(stubbedRetrievalResult),
    config = frontendAppConfig,
    parser = app.injector.instanceOf[BodyParsers.Default]
  )
  override def fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/test-url")



  "Auth Action" when {

    "the user has enrolled in PODS as a PSA must return OK" in {
        val authAction = action(Set(enrolmentPSA))
        val controller = new Harness(action = authAction)

        val result = controller.onPageLoad()(fakeRequest)
        status(result) mustBe OK
    }

    "the user has enrolled in PODS as a PSP must return OK" in {
      val authAction = action(Set(enrolmentPSP))
      val controller = new Harness(action = authAction)

      val result = controller.onPageLoad()(fakeRequest)
      status(result) mustBe OK
    }

    "the user has enrolled in PODS as a PSP and PSA must redirect to overview" in {
      val authAction = action(Set(enrolmentPSA, enrolmentPSP))
      val controller = new Harness(action = authAction)

      val result = controller.onPageLoad()(fakeRequest)
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.SchemesOverviewController.onPageLoad().url)
    }

    "the user has not enrolled in PODS should be redirected to login page" in {
      val authAction = action(Future.failed(new NoActiveSession("") {} ))
      val controller = new Harness(action = authAction)


      val result = controller.onPageLoad()(fakeRequest)
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(s"${frontendAppConfig.loginUrl}?continue=${URLEncoder.encode(frontendAppConfig.loginContinueUrl, "UTF-8")}")
    }
  }
}

object NoBothEnrolmentsOnlyAuthActionSpec extends SpecBase with MockitoSugar {

  private def authRetrievals(enrolments: Set[Enrolment]): Future[Some[String] ~ Enrolments] =
    Future.successful(new~(Some("id"), Enrolments(enrolments)))
}
