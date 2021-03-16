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
import connectors.UserAnswersCacheConnector
import controllers.routes
import identifiers.AdministratorOrPractitionerId
import models.{AdministratorOrPractitioner, AuthEntity}
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import models.AuthEntity.{PSP, PSA}
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc._
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, ~}
import uk.gov.hmrc.http.HeaderCarrier
import utils.UserAnswers

import scala.concurrent.{ExecutionContext, Future}

class AuthActionSpec
  extends SpecBase {

  import AuthActionSpec._

  "Auth Action" when {

    "the user has enrolled in PODS as a PSA" must {
      "return OK" in {
        val authAction = new AuthActionImpl(
          authConnector = fakeAuthConnector(authRetrievals(Set(enrolmentPSA))),
          mockUserAnswersCacheConnector,
          config = frontendAppConfig,
          parser = app.injector.instanceOf[BodyParsers.Default]
        )
        val controller = new Harness(authAction)

        val result = controller.onPageLoad()(fakeRequest)
        status(result) mustBe OK
      }
    }

    "the user has enrolled in PODS as a PSP" must {
      "return OK" in {
        val authAction = new AuthActionImpl(
          authConnector = fakeAuthConnector(authRetrievals(Set(enrolmentPSP))),
          mockUserAnswersCacheConnector,
          config = frontendAppConfig,
          parser = app.injector.instanceOf[BodyParsers.Default]
        )
        val controller = new Harness(authAction, authEntity = PSP)

        val result = controller.onPageLoad()(fakeRequest)
        status(result) mustBe OK
      }
    }

    "the user has enrolled in PODS as both a PSA AND a PSP" must {
      "have access to PSA page when he has chosen to act as a PSA" in {
        val optionUAJson = UserAnswers()
          .set(AdministratorOrPractitionerId)(AdministratorOrPractitioner.Administrator).asOpt.map(_.json)
        when(mockUserAnswersCacheConnector.fetch(any())(any(), any())).thenReturn(Future.successful(optionUAJson))
        val authAction = new AuthActionImpl(
          authConnector = fakeAuthConnector(authRetrievals(Set(enrolmentPSA, enrolmentPSP))),
          mockUserAnswersCacheConnector,
          config = frontendAppConfig,
          parser = app.injector.instanceOf[BodyParsers.Default]
        )
        val controller = new Harness(authAction, authEntity = PSA)

        val result = controller.onPageLoad()(fakeRequest)
        status(result) mustBe OK
      }

      "have access to PSP page when he has chosen to act as a PSP" in {
        val optionUAJson = UserAnswers()
          .set(AdministratorOrPractitionerId)(AdministratorOrPractitioner.Practitioner).asOpt.map(_.json)
        when(mockUserAnswersCacheConnector.fetch(any())(any(), any())).thenReturn(Future.successful(optionUAJson))
        val authAction = new AuthActionImpl(
          authConnector = fakeAuthConnector(authRetrievals(Set(enrolmentPSA, enrolmentPSP))),
          mockUserAnswersCacheConnector,
          config = frontendAppConfig,
          parser = app.injector.instanceOf[BodyParsers.Default]
        )
        val controller = new Harness(authAction, authEntity = PSP)

        val result = controller.onPageLoad()(fakeRequest)
        status(result) mustBe OK
      }

      "not have access to PSA page and be redirected to the administrator or practitioner page when he has NOT chosen to act as either a PSA or a PSP" in {
        when(mockUserAnswersCacheConnector.fetch(any())(any(), any())).thenReturn(Future.successful(Some(UserAnswers().json)))
        val authAction = new AuthActionImpl(
          authConnector = fakeAuthConnector(authRetrievals(Set(enrolmentPSA, enrolmentPSP))),
          mockUserAnswersCacheConnector,
          config = frontendAppConfig,
          parser = app.injector.instanceOf[BodyParsers.Default]
        )
        val controller = new Harness(authAction, authEntity = PSA)

        val result = controller.onPageLoad()(fakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.AdministratorOrPractitionerController.onPageLoad().url)
      }

      "not have access to PSP page and be redirected to the administrator or practitioner page when he has NOT chosen to act as either a PSA or a PSP" in {
        when(mockUserAnswersCacheConnector.fetch(any())(any(), any())).thenReturn(Future.successful(Some(UserAnswers().json)))
        val authAction = new AuthActionImpl(
          authConnector = fakeAuthConnector(authRetrievals(Set(enrolmentPSA, enrolmentPSP))),
          mockUserAnswersCacheConnector,
          config = frontendAppConfig,
          parser = app.injector.instanceOf[BodyParsers.Default]
        )
        val controller = new Harness(authAction, authEntity = PSP)

        val result = controller.onPageLoad()(fakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.AdministratorOrPractitionerController.onPageLoad().url)
      }

      "for a PSP page be redirected to the InterruptToAdministrator page when he has chosen to act as a PSA" in {
        val optionUAJson = UserAnswers()
          .set(AdministratorOrPractitionerId)(AdministratorOrPractitioner.Administrator).asOpt.map(_.json)
        when(mockUserAnswersCacheConnector.fetch(any())(any(), any())).thenReturn(Future.successful(optionUAJson))
        val authAction = new AuthActionImpl(
          authConnector = fakeAuthConnector(authRetrievals(Set(enrolmentPSA, enrolmentPSP))),
          mockUserAnswersCacheConnector,
          config = frontendAppConfig,
          parser = app.injector.instanceOf[BodyParsers.Default]
        )
        val controller = new Harness(authAction, authEntity = PSP)

        val result = controller.onPageLoad()(fakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.InterruptToAdministratorController.onPageLoad().url)
      }
      "for a PSA page be redirected to the CannotAccessPageAsPractitioner page when he has chosen to act as a PSP" in {
        val optionUAJson = UserAnswers()
          .set(AdministratorOrPractitionerId)(AdministratorOrPractitioner.Practitioner).asOpt.map(_.json)
        when(mockUserAnswersCacheConnector.fetch(any())(any(), any())).thenReturn(Future.successful(optionUAJson))
        val authAction = new AuthActionImpl(
          authConnector = fakeAuthConnector(authRetrievals(Set(enrolmentPSA, enrolmentPSP))),
          mockUserAnswersCacheConnector,
          config = frontendAppConfig,
          parser = app.injector.instanceOf[BodyParsers.Default]
        )
        val controller = new Harness(authAction, authEntity = PSA)

        val result = controller.onPageLoad()(fakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.CannotAccessPageAsPractitionerController.onPageLoad().url)
      }
    }



    "the user hasn't enrolled in PODS" must {
      "redirect the user to pension administrator frontend" in {
        val authAction = new AuthActionImpl(
          authConnector = fakeAuthConnector(authRetrievals()),
          mockUserAnswersCacheConnector,
          config = frontendAppConfig,
          parser = app.injector.instanceOf[BodyParsers.Default]
        )
        val controller = new Harness(authAction)

        val result = controller.onPageLoad()(fakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.YouNeedToRegisterController.onPageLoad().url)
      }
    }

    "the user hasn't logged in" must {
      "redirect the user to log in " in {
        val authAction = new AuthActionImpl(
          fakeAuthConnector(Future.failed(new MissingBearerToken)),
          mockUserAnswersCacheConnector,
          frontendAppConfig, app.injector.instanceOf[BodyParsers.Default]
        )
        val controller = new Harness(authAction)
        val result = controller.onPageLoad()(fakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get must startWith(frontendAppConfig.loginUrl)
      }
    }

    "the user's session has expired" must {
      "redirect the user to log in " in {
        val authAction = new AuthActionImpl(
          fakeAuthConnector(Future.failed(new BearerTokenExpired)),
          mockUserAnswersCacheConnector,
          frontendAppConfig, app.injector.instanceOf[BodyParsers.Default]
        )
        val controller = new Harness(authAction)
        val result = controller.onPageLoad()(fakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get must startWith(frontendAppConfig.loginUrl)
      }
    }

    "the user doesn't have sufficient enrolments" must {
      "redirect the user to the unauthorised page" in {
        val authAction = new AuthActionImpl(
          fakeAuthConnector(Future.failed(new InsufficientEnrolments)),
          mockUserAnswersCacheConnector,
          frontendAppConfig, app.injector.instanceOf[BodyParsers.Default]
        )
        val controller = new Harness(authAction)
        val result = controller.onPageLoad()(fakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad().url)
      }
    }

    "the user doesn't have sufficient confidence level" must {
      "redirect the user to the unauthorised page" in {
        val authAction = new AuthActionImpl(
          fakeAuthConnector(Future.failed(new InsufficientConfidenceLevel)),
          mockUserAnswersCacheConnector,
          frontendAppConfig, app.injector.instanceOf[BodyParsers.Default]
        )
        val controller = new Harness(authAction)
        val result = controller.onPageLoad()(fakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad().url)
      }
    }

    "the user used an unaccepted auth provider" must {
      "redirect the user to the unauthorised page" in {
        val authAction = new AuthActionImpl(
          fakeAuthConnector(Future.failed(new UnsupportedAuthProvider)),
          mockUserAnswersCacheConnector,
          frontendAppConfig, app.injector.instanceOf[BodyParsers.Default]
        )
        val controller = new Harness(authAction)
        val result = controller.onPageLoad()(fakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad().url)
      }
    }

    "the user has an unsupported affinity group" must {
      "redirect the user to the unauthorised page" in {
        val authAction = new AuthActionImpl(
          fakeAuthConnector(Future.failed(new UnsupportedAffinityGroup)),
          mockUserAnswersCacheConnector,
          frontendAppConfig, app.injector.instanceOf[BodyParsers.Default]
        )
        val controller = new Harness(authAction)
        val result = controller.onPageLoad()(fakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad().url)
      }
    }

    "the user has an unsupported credential role" must {
      "redirect the user to the unauthorised page" in {
        val authAction = new AuthActionImpl(
          fakeAuthConnector(Future.failed(new UnsupportedCredentialRole)),
          mockUserAnswersCacheConnector,
          frontendAppConfig, app.injector.instanceOf[BodyParsers.Default]
        )
        val controller = new Harness(authAction)
        val result = controller.onPageLoad()(fakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad().url)
      }
    }
  }
}

object AuthActionSpec extends SpecBase with MockitoSugar {

  private val enrolmentPSP = Enrolment(
    key = "HMRC-PODSPP-ORG",
    identifiers = Seq(EnrolmentIdentifier(key = "PSPID", value = "20000000")),
    state = "",
    delegatedAuthRule = None
  )

  private  val enrolmentPSA = Enrolment(
    key = "HMRC-PODS-ORG",
    identifiers = Seq(EnrolmentIdentifier(key = "PSAID", value = "A0000000")),
    state = "",
    delegatedAuthRule = None
  )

  private val mockUserAnswersCacheConnector = mock[UserAnswersCacheConnector]

  private class Harness(authAction: AuthAction, val controllerComponents: MessagesControllerComponents = controllerComponents,
    authEntity: AuthEntity = PSA)
    extends BaseController {
    def onPageLoad(): Action[AnyContent] = authAction.apply(authEntity) { _ => Ok }
  }

  private def fakeAuthConnector(stubbedRetrievalResult: Future[_]): AuthConnector = new AuthConnector {

    def authorise[A](predicate: Predicate, retrieval: Retrieval[A])
                    (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
      stubbedRetrievalResult.map(_.asInstanceOf[A])(ec)
  }

  private def authRetrievals(enrolments: Set[Enrolment] = Set()): Future[Some[String] ~ Enrolments ~ Some[AffinityGroup.Individual.type]] =
    Future.successful(new ~(new ~(Some("id"), Enrolments(enrolments)), Some(AffinityGroup.Individual)))
}
