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

package controllers.invitations.psa

import base.JsonFileReader
import connectors.{InvitationConnector, NameMatchingFailedException, PsaAlreadyInvitedException}
import connectors.scheme.SchemeDetailsConnector
import controllers.actions.{AuthAction, DataRetrievalAction, FakeAuthAction}
import controllers.behaviours.ControllerWithNormalPageBehaviours
import controllers.invitations.psa.routes._
import models.invitations.Invitation
import models.{AcceptedInvitation, MinimalSchemeDetail}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.Configuration
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}
import utils.{CheckYourAnswersFactory, UserAnswers}
import utils.countryOptions.CountryOptions
import viewmodels.AnswerSection
import views.html.check_your_answers_view

import scala.concurrent.{ExecutionContext, Future}

class CheckYourAnswersControllerSpec extends ControllerWithNormalPageBehaviours with MockitoSugar {

  import CheckYourAnswersControllerSpec._

  behave like controllerWithOnPageLoadMethod(onPageLoadAction, getEmptyData, Some(userAnswer), viewAsString)

  behave like controllerWithOnSubmitMethod(onSubmitAction, getEmptyData, Some(userAnswerUpdated), None)

  "calling submit" must {

    "redirect to duplicate invitation page if invitation failed with Psa already invited error" in {
      val result = onSubmitAction(userAnswerUpdated, FakeAuthAction, Future.failed(new PsaAlreadyInvitedException))(FakeRequest())

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(InvitationDuplicateController.onPageLoad().url)
    }

    "redirect to incorrect psa details page if invitation failed with name matching error" in {
      val result = onSubmitAction(userAnswerUpdated, FakeAuthAction, Future.failed(new NameMatchingFailedException))(FakeRequest())

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(IncorrectPsaDetailsController.onPageLoad().url)
    }

    "redirect to incorrect psa details page if psa id not found" in {
      val result = onSubmitAction(userAnswerUpdated, FakeAuthAction, Future.failed(new NotFoundException("not found")))(FakeRequest())

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(IncorrectPsaDetailsController.onPageLoad().url)
    }

    "redirect to psa already invited page if scheme already has invitee psa id associated with it and names match" in {

      val result = onSubmitAction(userAnswerUpdatedPsaAlreadyInvited, FakeAuthAction, Future.successful(()))(FakeRequest())
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(PsaAlreadyAssociatedController.onPageLoad().url)
    }

    "redirect to incorrect psa details page if scheme already has invitee psa id associated with it and names don't match" in {

      val result = onSubmitAction(userAnswerUpdatedPsaAlreadyInvited, FakeAuthAction, Future.failed(new NameMatchingFailedException))(FakeRequest())
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(IncorrectPsaDetailsController.onPageLoad().url)
    }
  }
}

object CheckYourAnswersControllerSpec extends ControllerWithNormalPageBehaviours with MockitoSugar with JsonFileReader {
  private val testSrn: String = "test-srn"
  private val testPstr = "test-pstr"
  private val testSchemeName = "test-scheme-name"
  private val testSchemeDetail = MinimalSchemeDetail(testSrn, Some(testPstr), testSchemeName)
  private val srn = "S9000000000"

  private val userAnswer = UserAnswers()
    .minimalSchemeDetails(testSchemeDetail)
    .dataRetrievalAction

  private val userAnswerUpdated = UserAnswers()
    .minimalSchemeDetails(testSchemeDetail)
    .inviteeId("A7654321")
    .inviteeName("test-invite-name")
    .dataRetrievalAction

  private val userAnswerUpdatedPsaAlreadyInvited = UserAnswers()
    .minimalSchemeDetails(testSchemeDetail)
    .inviteeId("A0000000")
    .inviteeName("test-invite-name")
    .srn(srn)
    .dataRetrievalAction

  private val countryOptions = new CountryOptions(environment, frontendAppConfig)
  private val checkYourAnswersFactory = new CheckYourAnswersFactory(countryOptions)

  private val fakeSchemeDetailsConnector: SchemeDetailsConnector = mock[SchemeDetailsConnector]
  val config = injector.instanceOf[Configuration]
  private val view = injector.instanceOf[check_your_answers_view]

  private def fakeInvitationConnector(response: Future[Unit] = Future.successful(())): InvitationConnector = new InvitationConnector {

    override def invite(invitation: Invitation)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = response

    override def acceptInvite(acceptedInvitation: AcceptedInvitation)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = ???
  }

  def call: Call = CheckYourAnswersController.onSubmit()

  def viewAsString() = view(Seq(), call,
    Some("messages__check__your__answer__main__containt__label"), Some(testSchemeName))(fakeRequest, messages).toString

  def onPageLoadAction(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction) = {

    new CheckYourAnswersController(
      frontendAppConfig, messagesApi, fakeAuth, navigator, dataRetrievalAction, requiredDateAction,
      checkYourAnswersFactory, fakeSchemeDetailsConnector, fakeInvitationConnector(), controllerComponents, view).onPageLoad()
  }

  def onSubmitAction(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction) = {

    when(fakeSchemeDetailsConnector.getSchemeDetails(any(), any(), any())(any(), any()))
      .thenReturn(Future.successful(UserAnswers(readJsonFromFile("/data/validSchemeDetailsResponse.json"))))

    new CheckYourAnswersController(
      frontendAppConfig, messagesApi, fakeAuth, navigator, dataRetrievalAction, requiredDateAction,
      checkYourAnswersFactory, fakeSchemeDetailsConnector, fakeInvitationConnector(), controllerComponents, view).onSubmit()
  }

  def onSubmitAction(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction, invitationResponse: Future[Unit]) = {

        when(fakeSchemeDetailsConnector.getSchemeDetails(any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(UserAnswers(readJsonFromFile("/data/validSchemeDetailsResponse.json"))))

    new CheckYourAnswersController(
      frontendAppConfig, messagesApi, fakeAuth, navigator, dataRetrievalAction, requiredDateAction,
      checkYourAnswersFactory, fakeSchemeDetailsConnector, fakeInvitationConnector(invitationResponse), controllerComponents, view).onSubmit()
  }
}
