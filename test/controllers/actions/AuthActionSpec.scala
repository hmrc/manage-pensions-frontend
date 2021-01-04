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

package controllers.actions

import base.SpecBase
import controllers.routes
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.BaseController
import play.api.mvc.BodyParsers
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class AuthActionSpec extends SpecBase {

  import AuthActionSpec._

  "Auth Action" when {

    "the user hasn't enrolled in PODS" must {
      "redirect the user to pension administrator frontend" in {
        val authAction = new AuthActionImpl(fakeAuthConnector(authRetrievals), frontendAppConfig, app.injector.instanceOf[BodyParsers.Default])
        val controller = new Harness(authAction)

        val result = controller.onPageLoad()(fakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.YouNeedToRegisterController.onPageLoad().url)
      }
    }

    "the user hasn't logged in" must {
      "redirect the user to log in " in {
        val authAction = new AuthActionImpl(fakeAuthConnector(Future.failed(new MissingBearerToken)), frontendAppConfig, app.injector.instanceOf[BodyParsers.Default])
        val controller = new Harness(authAction)
        val result = controller.onPageLoad()(fakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get must startWith(frontendAppConfig.loginUrl)
      }
    }

    "the user's session has expired" must {
      "redirect the user to log in " in {
        val authAction = new AuthActionImpl(fakeAuthConnector(Future.failed(new BearerTokenExpired)), frontendAppConfig, app.injector.instanceOf[BodyParsers.Default])
        val controller = new Harness(authAction)
        val result = controller.onPageLoad()(fakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get must startWith(frontendAppConfig.loginUrl)
      }
    }

    "the user doesn't have sufficient enrolments" must {
      "redirect the user to the unauthorised page" in {
        val authAction = new AuthActionImpl(fakeAuthConnector(Future.failed(new InsufficientEnrolments)), frontendAppConfig, app.injector.instanceOf[BodyParsers.Default])
        val controller = new Harness(authAction)
        val result = controller.onPageLoad()(fakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad().url)
      }
    }

    "the user doesn't have sufficient confidence level" must {
      "redirect the user to the unauthorised page" in {
        val authAction = new AuthActionImpl(fakeAuthConnector(Future.failed(new InsufficientConfidenceLevel)), frontendAppConfig, app.injector.instanceOf[BodyParsers.Default])
        val controller = new Harness(authAction)
        val result = controller.onPageLoad()(fakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad().url)
      }
    }

    "the user used an unaccepted auth provider" must {
      "redirect the user to the unauthorised page" in {
        val authAction = new AuthActionImpl(fakeAuthConnector(Future.failed(new UnsupportedAuthProvider)), frontendAppConfig, app.injector.instanceOf[BodyParsers.Default])
        val controller = new Harness(authAction)
        val result = controller.onPageLoad()(fakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad().url)
      }
    }

    "the user has an unsupported affinity group" must {
      "redirect the user to the unauthorised page" in {
        val authAction = new AuthActionImpl(fakeAuthConnector(Future.failed(new UnsupportedAffinityGroup)), frontendAppConfig, app.injector.instanceOf[BodyParsers.Default])
        val controller = new Harness(authAction)
        val result = controller.onPageLoad()(fakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad().url)
      }
    }

    "the user has an unsupported credential role" must {
      "redirect the user to the unauthorised page" in {
        val authAction = new AuthActionImpl(fakeAuthConnector(Future.failed(new UnsupportedCredentialRole)), frontendAppConfig, app.injector.instanceOf[BodyParsers.Default])
        val controller = new Harness(authAction)
        val result = controller.onPageLoad()(fakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad().url)
      }
    }
  }
}

object AuthActionSpec {
  private def fakeAuthConnector(stubbedRetrievalResult: Future[_]): AuthConnector = new AuthConnector {

    def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] = {
      stubbedRetrievalResult.map(_.asInstanceOf[A])
    }
  }

  private def authRetrievals: Future[Some[String] ~ Enrolments ~ Some[AffinityGroup.Individual.type]] =
    Future.successful(new ~(new ~(Some("id"), Enrolments(Set())), Some(AffinityGroup.Individual)))

  class Harness(authAction: AuthAction, val controllerComponents: MessagesControllerComponents = stubMessagesControllerComponents()) extends BaseController {
    def onPageLoad(): Action[AnyContent] = authAction.apply() { _ => Ok }
  }

}
