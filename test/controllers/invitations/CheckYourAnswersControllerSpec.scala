/*
 * Copyright 2018 HM Revenue & Customs
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

package controllers.invitations

import connectors.InvitationConnector
import controllers.actions.{FakeAuthAction, AuthAction, DataRetrievalAction}
import controllers.behaviours.ControllerWithNormalPageBehaviours
import models.MinimalSchemeDetail
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.{CheckYourAnswersFactory, UserAnswers}
import viewmodels.AnswerSection
import views.html.check_your_answers

import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends ControllerWithNormalPageBehaviours with MockitoSugar{
  //scalastyle:off magic.number
  private val testSrn: String = "test-srn"
  private val testPstr = "test-pstr"
  private val testSchemeName = "test-scheme-name"
  private val testSchemeDetail = MinimalSchemeDetail(testSrn, Some(testPstr), testSchemeName)

  private val mockInvitationConnector = mock[InvitationConnector]

  when(mockInvitationConnector.invite(any())(any(), any())).thenReturn(Future.successful(201))

  private lazy val continue: Call = controllers.invitations.routes.InvitationSuccessController.onSubmit

  private val userAnswer = UserAnswers()
    .minimalSchemeDetails(testSchemeDetail)
    .dataRetrievalAction

  private val userAnswerUpdated = UserAnswers()
    .minimalSchemeDetails(testSchemeDetail)
    .inviteeId("test-invite-id")
    .inviteeName("test-invite-name")
    .dataRetrievalAction

  private val checkYourAnswersFactory = new CheckYourAnswersFactory()

  def call: Call = controllers.invitations.routes.CheckYourAnswersController.onSubmit()

  def viewAsString() = check_your_answers(frontendAppConfig, Seq(AnswerSection(None, Seq())), None, call,
    Some("messages__check__your__answer__main__containt__label"), Some(testSchemeName))(fakeRequest, messages).toString

  def onPageLoadAction(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction) = {

    new CheckYourAnswersController(
      frontendAppConfig, messagesApi, fakeAuth, navigator, dataRetrievalAction, requiredDateAction,
      checkYourAnswersFactory, mockInvitationConnector).onPageLoad()
  }

  def onSubmitAction(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction) = {

    new CheckYourAnswersController(
      frontendAppConfig, messagesApi, fakeAuth, navigator, dataRetrievalAction, requiredDateAction,
      checkYourAnswersFactory, mockInvitationConnector).onSubmit()
  }

  def redirectionCall() = controllers.invitations.routes.InvitationSuccessController.onPageLoad

  behave like controllerWithOnPageLoadMethod(onPageLoadAction, getEmptyData, Some(userAnswer), viewAsString)

  behave like controllerWithOnSubmitMethod(onSubmitAction, getEmptyData,  Some(userAnswerUpdated), Some(redirectionCall))

  "calling submit" must {

    "redirect to session expired page if invitation was not created" in {

      when(mockInvitationConnector.invite(any())(any(), any())).thenReturn(Future.successful(400))

      val result = onSubmitAction(userAnswerUpdated, FakeAuthAction())(FakeRequest())

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.SessionExpiredController.onPageLoad().url)
    }
  }
}
